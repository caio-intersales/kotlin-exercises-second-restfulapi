package de.intersales.quickstep.products.mapper

import de.intersales.quickstep.products.dto.CreateProductDto
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.dto.UpdateProductDto
import de.intersales.quickstep.products.entity.ProductsEntity
import de.intersales.quickstep.users.dto.UsersDto
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ProductsMapper {
    /**
     * Convert create DTO to Entity data
     */
    fun createDataToEntity(dto: CreateProductDto): ProductsEntity {
        return ProductsEntity().apply{
            this.productName = dto.productName
            this.productType = dto.productType
            this.productPrice = dto.productPrice
            this.productQnt = dto.productQnt
        }
    }

    /**
     * Convert update DTO to Entity data
     */
    fun updateDataToEntity(existingEntity: ProductsEntity, dto: UpdateProductDto){
        if(dto.productName != null){
            existingEntity.productName = dto.productName
        }

        if(dto.productType != null){
            existingEntity.productType = dto.productType
        }

        if(dto.productPrice != null){
            existingEntity.productPrice = dto.productPrice
        }

        if(dto.productQnt != null){
            existingEntity.productQnt = dto.productQnt
        }
    }

    /**
     * Convert Entity data to DTO data
     */
    fun entityToDto(entity: ProductsEntity?): ProductsDto {
        return ProductsDto(
            id          = entity?.id,
            productName  = entity?.productName ?: "",
            productType  = entity?.productType ?: 0,
            productPrice = entity?.productPrice ?: 0.00,
            productQnt   = entity?.productQnt ?: 0
        )
    }
}