package de.intersales.quickstep.users.mapper

import de.intersales.quickstep.users.dto.CreateUserDto
import de.intersales.quickstep.users.dto.UpdateUserDto
import de.intersales.quickstep.users.dto.UsersDto
import de.intersales.quickstep.users.entity.UsersEntity
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class UsersMapper {

    /**
     * Convert create DTO to Entity data
     */
    fun createDataToEntity(dto: CreateUserDto): UsersEntity {
        return UsersEntity().apply{
            this.firstName  = dto.firstName
            this.lastName   = dto.lastName
            this.emailAddress = dto.email
            // Password will be hashed in Services
            this.deliveryAddress = dto.deliveryAddress
        }
    }

    /**
     * Convert update DTO to Entity data
     */
    fun updateDataToEntity(existingEntity: UsersEntity, dto: UpdateUserDto) {

        if(dto.firstName != null){
            existingEntity.firstName = dto.firstName
        }

        if(dto.lastName != null){
            existingEntity.lastName = dto.lastName
        }

        if(dto.email != null){
            existingEntity.emailAddress = dto.email
        }

        if(dto.deliveryAddress != null){
            existingEntity.deliveryAddress = dto.deliveryAddress
        }
    }

    /**
     * Convert Entity data to DTO data
     */

    fun entityToDto(entity: UsersEntity?): UsersDto {
        return UsersDto(
            id          = entity?.id,
            // If the values are null, it will be left blank
            firstName   = entity?.firstName ?: "",
            lastName    = entity?.lastName ?: "",
            email       = entity?.emailAddress ?: "",
            // Value can be null from the DTO
            deliveryAddress = entity?.deliveryAddress
        )
    }

}