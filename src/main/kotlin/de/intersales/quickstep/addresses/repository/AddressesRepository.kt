package de.intersales.quickstep.addresses.repository

import de.intersales.quickstep.addresses.entity.AddressesEntity
import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Multi
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AddressesRepository : PanacheRepository<AddressesEntity> {

    /**
     * Function: findByCountry
     * What does it do: Allows for searching addresses based on country
     */
    fun findByCountry(countryCode: String): Multi<AddressesEntity?> {
        return find("country = :country", Parameters.with("country", countryCode))
            .stream<AddressesEntity>()
    }
}