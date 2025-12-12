package de.intersales.quickstep.addresses.mapper

import de.intersales.quickstep.addresses.dto.AddressesDto
import de.intersales.quickstep.addresses.dto.CreateAddressDto
import de.intersales.quickstep.addresses.dto.UpdateAddressDto
import de.intersales.quickstep.addresses.entity.AddressesEntity
import de.intersales.quickstep.users.dto.CreateUserDto
import de.intersales.quickstep.users.dto.UpdateUserDto
import de.intersales.quickstep.users.dto.UsersDto
import de.intersales.quickstep.users.entity.UsersEntity
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AddressesMapper {
    /**
     * Convert create DTO to Entity data
     */
    fun createDataToEntity(dto: CreateAddressDto): AddressesEntity {
        return AddressesEntity().apply{
            this.userId     = dto.userId
            this.street     = dto.street
            this.houseNumber= dto.houseNumber
            this.city       = dto.city
            this.state      = dto.state
            this.zip        = dto.zip
            this.country    = dto.country
        }
    }

    /**
     * Convert update DTO to Entity data
     */
    fun updateDataToEntity(existingEntity: AddressesEntity, dto: UpdateAddressDto) {

        if(dto.street != null){
            existingEntity.street = dto.street
        }

        if(dto.houseNumber != null){
            existingEntity.houseNumber = dto.houseNumber
        }

        if(dto.city != null){
            existingEntity.city = dto.city
        }

        if(dto.state != null){
            existingEntity.state = dto.state
        }

        if(dto.zip != null){
            existingEntity.zip = dto.zip
        }

        if(dto.country != null){
            existingEntity.country = dto.country
        }
    }

    /**
     * Convert Entity data to DTO data
     */

    fun entityToDto(entity: AddressesEntity?): AddressesDto {
        return AddressesDto(
            id          = entity?.id,
            userId      = entity?.userId,
            // If the values are null, it will be left blank
            street      = entity?.street ?: "",
            houseNumber = entity?.houseNumber ?: "",
            city        = entity?.city ?: "",
            state       = entity?.state ?: "",
            zip         = entity?.zip ?: "",
            country     = entity?.country ?: ""
        )
    }
}