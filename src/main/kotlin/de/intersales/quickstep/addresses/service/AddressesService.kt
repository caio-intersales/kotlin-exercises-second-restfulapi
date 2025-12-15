package de.intersales.quickstep.addresses.service

import de.intersales.quickstep.addresses.dto.AddressesDto
import de.intersales.quickstep.addresses.dto.CreateAddressDto
import de.intersales.quickstep.addresses.entity.AddressesEntity
import de.intersales.quickstep.addresses.mapper.AddressesMapper
import de.intersales.quickstep.addresses.repository.AddressesRepository
import de.intersales.quickstep.exceptions.ElementNotFoundException
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AddressesService (
    private val addressesRepository: AddressesRepository,
    private val addressesMapper: AddressesMapper
    ) {

    /**
     * Function: createNewAddress
     * What does it do: the function receives data from DTO, converts it into entity data (and back), sending it to the repository to be saved to the DB
     */
    fun createNewAddress(dto: CreateAddressDto): Uni<AddressesDto> {
        val newEntity = addressesMapper.createDataToEntity(dto)

        return addressesRepository.persistAndFlush(newEntity)
            .onItem().transform {
                addressesMapper.entityToDto(newEntity)
            }
    }

    /**
     * Function findByUser
     * What does it do: the function receives the ID of a user and returns the address connected to them
     */
    fun findByUser(id: Long): Uni<AddressesDto> {
        return addressesRepository.findByUser(id)
            .onItem().ifNull()
            .failWith { ElementNotFoundException("An address could not be found with the given ID") }
            .map(addressesMapper::entityToDto)
    }

    /**
     * Function: findByCountry
     * What does it do: the function receives a country code (String) and returns all addresses that are associated to that country code
     */
    fun findByCountry(countryCode: String): Uni<List<AddressesDto>> {
        val query: Uni<List<AddressesEntity>> = addressesRepository.findByCountry(countryCode)

        return query
            .onItem().transform { entityList ->
                entityList.map { entity ->
                    addressesMapper.entityToDto(entity)
                }
            }
    }

}