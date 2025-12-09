package de.intersales.quickstep.users.repository

import de.intersales.quickstep.users.entity.UsersEntity
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class UsersRepository : PanacheRepository<UsersEntity>{
    /**
     * Function: findByEmail
     * What does it do: Allows for searching for users by their email address (which should be unique)
     */
    fun findByEmail(email: String): Uni<UsersEntity?>{
        return find("emailaddress = :email", Parameters.with("email", email))
            .firstResult<UsersEntity>()
    }

    /**
     * Function: showAllUsers
     * What does it do: Reads all the users saved in the database and returns them
     */
    fun showAllUsers(): PanacheQuery<UsersEntity> {
        return findAll()
    }

    /**
     * Function: findUserById
     * What does it do: Allows for searching for users based on their ID (unique)
     */
    fun findUserById(id: Long): Uni<UsersEntity?> {
        return find("id = :id", Parameters.with("id", id)).singleResult()
    }

    /**
     * Function: findListOfUsers
     * What does it do: Allows for searching for many users based on their IDs
     */
    fun findListOfUsers(ids: Collection<Long?>): Uni<List<UsersEntity>> {
        return list("id in :ids", Parameters.with("ids", ids))
    }

    /**
     * Other functions ...
     */
}