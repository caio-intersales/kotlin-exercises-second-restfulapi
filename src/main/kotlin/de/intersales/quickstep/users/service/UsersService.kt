package de.intersales.quickstep.users.service

import de.intersales.quickstep.users.dto.CreateUserDto
import de.intersales.quickstep.users.dto.UpdateUserDto
import de.intersales.quickstep.users.dto.UsersDto
import de.intersales.quickstep.users.entity.UsersEntity
import de.intersales.quickstep.users.exception.DuplicateUserException
import de.intersales.quickstep.users.exception.ElementNotFoundException
import de.intersales.quickstep.users.mapper.UsersMapper
import de.intersales.quickstep.users.repository.UsersRepository
import de.intersales.quickstep.util.sha256Hash
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.smallrye.mutiny.Uni
import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class UsersService(
    private val usersRepository: UsersRepository,
    private val usersMapper: UsersMapper
    ) {

    private val logger = Logger.getLogger(javaClass.name)

    /**
     * Function: createNewUser
     * What does it do: the function receives data from DTO and check for email duplicates (throwing an error if it's already in use), converts DTO into entity data (and back), send data to the database (via repository)
    **/

    fun createNewUser(dto: CreateUserDto): Uni<UsersDto> {

        // Start the result already with a return
        // 1. Check whether the email is already stored in the database
        return usersRepository.findByEmail(dto.email)
            .onItem().transformToUni { existingUser ->
                    // As the result is an actual user-entry in the database, check whether it is null (doesn't exist)
                    if(existingUser != null){
                        // If it is found, then throw an error
                        return@transformToUni Uni.createFrom().failure(
                            DuplicateUserException("The email address '${dto.email}' is already in use.")
                        )
                    }
                logger.info("No existing user found")

                // If no user is found, then continue to add the user to the database

                // 2. Convert DTO to Entity data
                // The password however is not hashed here, as Mapper doesn't handle it
                val newUserEntity = usersMapper.createDataToEntity(dto)

                // 3. Hash password
                val hashedPassword = sha256Hash(dto.rawPassword)

                // Store the hashed password to the new User entity object
                newUserEntity.password = hashedPassword

                // 4. Save the new entity
                return@transformToUni usersRepository.persistAndFlush(newUserEntity)
                    // Wait until the new user has been saved, then map it
                    .onItem().transform {
                        // 5. Convert the saved entity back to the DTO data for the response
                        usersMapper.entityToDto(newUserEntity)
                    }
            }
    }

    /**
     * Function: findAllUsers
     * What does it do: the function reads all users from the database and return them, transforming Entity data into DTO
     */

    fun showAllUsers(): Uni<List<UsersDto>> {
        // 1. Get the PanacheQuery object from the repository
        val query: PanacheQuery<UsersEntity> = usersRepository.showAllUsers()

        // 2. Execute the query and get an adequate response
        return query.list<UsersEntity>()
            .onItem().transform { entityList ->
                entityList.map { entity ->
                    usersMapper.entityToDto(entity)
                }
            }
    }

    /**
     * Function: findOneUser
     * What does it do: the function retrieves data from a specific user based on their ID and returns it as DTO
     */

    fun findOneUser(id: Long): Uni<UsersDto> {
        return usersRepository.findUserById(id)
            .onItem().ifNotNull().transform { entity ->
                usersMapper.entityToDto(entity)
            }
            .onItem().ifNull().failWith {
                ElementNotFoundException("The user with ID $id was not found.")
            }
    }

    /**
     * Function: updateUser
     * What does it do: the function passes user data to be updated
     */
    fun updateOneUser(dto: UpdateUserDto): Uni<UsersDto> {
        // Start the result already with a return
        // 1. Check whether this user exists
        return usersRepository.findUserById(dto.id)
            .onItem().transformToUni { existingUser ->
                // If no user is found, throw an error
                if(existingUser == null){
                    return@transformToUni Uni.createFrom().failure(
                        ElementNotFoundException("The user with the id '${dto.id}' was not found.")
                    )
                }

                // 2. Convert DTO to Entity data
                // No password is changed here
                usersMapper.updateDataToEntity(existingUser, dto)

                // Update is automatically done when the transaction commits
                // So here the managed entity is returned
                return@transformToUni Uni.createFrom().item(usersMapper.entityToDto(existingUser))
            }
    }

    /**
     * Function: deleteUser
     * What does it do: the function deletes a user based on their ID
     */
    fun deleteUser(id: Long): Uni<Boolean> {
        return usersRepository.deleteById(id) // Uses a Panache inherited method
            .onItem().transform { success ->
                if(!success){
                    throw ElementNotFoundException("The user with the id '${id}' was not found.")
                }
                true
            }
    }

}