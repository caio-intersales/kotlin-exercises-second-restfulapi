package de.intersales.quickstep.orders.mapper

import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.dto.UpdateOrderDto
import de.intersales.quickstep.orders.entity.OrdersEntity
import de.intersales.quickstep.products.dto.ProductsDto
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class OrdersMapper {

    /**
     * Convert create DTO to Entity data
     */
    fun createDataToEntity(dto: CreateOrderDto): OrdersEntity {
        return OrdersEntity().apply{
            this.orderOwner  = dto.orderOwner
            this.setOrderProducts(dto.orderProducts)
        }
    }

    /**
     * Convert update DTO to Entity data
     */
    fun updateDataToEntity(existingEntity: OrdersEntity, dto: UpdateOrderDto) {
        existingEntity.orderOwner = dto.orderOwner
        existingEntity.setOrderProducts(dto.orderProducts)
    }

    /**
     * Convert Entity data to DTO data
     */

    fun entityToDto(entity: OrdersEntity?): OrdersDto {
        if(entity == null){
            throw IllegalArgumentException("Cannot convert null entity to DTO.")
        }

        return OrdersDto(
            id = entity.id,
            orderOwner = null,
            orderProducts = emptyList<ProductsDto>(),
            issueDate = entity.issueDate
        )
    }
}