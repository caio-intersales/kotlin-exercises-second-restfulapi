package de.intersales.quickstep.users.repository

import de.intersales.quickstep.users.entity.UsersEntity
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Uni
import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class UsersRepository : PanacheRepository<UsersEntity>{
    private val logger = Logger.getLogger(javaClass.name)
    /**
     * Function: findByEmail
     * What does it do: Allows for searching for users by their email address (which should be unique)
     */
    fun findByEmail(email: String): Uni<UsersEntity?>{
logger.info("Start looking for user by email: $email")
        return find("emailaddress = :email", Parameters.with("email", email))
            .list<UsersEntity>()
            .onItem().transform { it.firstOrNull() }
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
     * Other functions ...
     */
}