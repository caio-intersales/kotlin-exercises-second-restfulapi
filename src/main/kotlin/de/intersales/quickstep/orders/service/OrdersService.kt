package de.intersales.quickstep.orders.service

import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.entity.OrdersEntity
import de.intersales.quickstep.orders.mapper.OrdersMapper
import de.intersales.quickstep.orders.repository.OrdersRepository
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.smallrye.mutiny.Uni
import java.time.OffsetDateTime
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class OrdersService (
    private val ordersRepository: OrdersRepository,
    private val ordersMapper: OrdersMapper
) {

    /**
     * Function: createNewOrder
     * What does it do: The function receives DTO and run the functions to insert it into the DB
     */

    fun createNewOrder(dto: CreateOrderDto): Uni<OrdersDto> {
        val newEntity = ordersMapper.createDataToEntity(dto)

        return ordersRepository.persistAndFlush(newEntity)
            .onItem().transform {
                ordersMapper.entityToDto(newEntity)
            }
    }

    /**
     * Function: findAllOrders
     * What does it do: The function returns all orders saved in the DB, independent of Owner
     */
    fun findAllOrders(): Uni<List<OrdersDto>> {
        val query: PanacheQuery<OrdersEntity> = ordersRepository.findAll()

        return query.list<OrdersEntity>()
            .onItem().transform { entityList ->
                entityList.map { entity ->
                    ordersMapper.entityToDto(entity)
                }
            }
    }

    /**
     * Function: findAllOrdersByOwner
     * What does it do: The function returns all orders issued by a specific user
     */
    fun findAllOrdersByOwner(orderOwner: Long): Uni<List<OrdersDto>> {
        return ordersRepository.findByOwner(orderOwner)
            .onItem().transform { entityList: List<OrdersEntity> ->
                entityList.map { entity ->
                    ordersMapper.entityToDto(entity)
                }
            }
    }

    /**
     * Function: findOrdersByDate
     * What does it do: it looks for all orders issued either before, after, or between given dates
     */
    fun findOrdersByDate(startDate: OffsetDateTime?, endDate: OffsetDateTime?): Uni<List<OrdersDto>> {
        return ordersRepository.findByDates(startDate, endDate)
            .onItem().transform { entityList: List<OrdersEntity> ->
                entityList.map { entity ->
                    ordersMapper.entityToDto(entity)
                }
            }
    }

    /**
     * Function: findOrdersByDateAndOwner
     * What does it do: it looks for all orders issued either before, after, or between given dates belonging to one owner
     */
    fun findOrdersByDateAndOwner(orderOwner: Long, startDate: OffsetDateTime?, endDate: OffsetDateTime?): Uni<List<OrdersDto>> {
        return ordersRepository.findByDatesAndOwner(orderOwner, startDate, endDate)
            .onItem().transform { entityList: List<OrdersEntity> ->
                entityList.map { entity ->
                    ordersMapper.entityToDto(entity)
                }
            }
    }
}