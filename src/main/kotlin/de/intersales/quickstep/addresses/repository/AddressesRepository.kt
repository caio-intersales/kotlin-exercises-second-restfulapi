package de.intersales.quickstep.addresses.repository

import de.intersales.quickstep.addresses.entity.AddressesEntity
import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AddressesRepository : PanacheRepository<AddressesEntity> {

    /**
     * Function: findByCountry
     * What does it do: Allows for searching addresses based on country
     */
    fun findByCountry(countryCode: String): Uni<List<AddressesEntity>> {
        return find("country = :country", Parameters.with("country", countryCode))
            .list()
    }

    /**
     * Function: findByUser
     * What does it do: Allows for searching an address based on an ID from a user
     */
    fun findByUser(userId: Long): Uni<AddressesEntity> {
        return find("user_id = :userId", Parameters.with("userId", userId))
            .singleResult()
    }
}