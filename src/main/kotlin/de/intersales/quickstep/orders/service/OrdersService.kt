package de.intersales.quickstep.orders.service

import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.entity.OrdersEntity
import de.intersales.quickstep.orders.mapper.OrdersMapper
import de.intersales.quickstep.orders.repository.OrdersRepository
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.entity.ProductsEntity
import de.intersales.quickstep.products.mapper.ProductsMapper
import de.intersales.quickstep.products.repository.ProductsRepository
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.smallrye.mutiny.Uni
import java.time.OffsetDateTime
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class OrdersService (
    private val ordersRepository: OrdersRepository,
    private val ordersMapper: OrdersMapper,
    private val productsRepository: ProductsRepository,
    private val productsMapper: ProductsMapper
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
     * It also returns the data from each product in the list (ProductsDTOs)
     */
    fun findAllOrders(): Uni<List<OrdersDto>> {
        val query: PanacheQuery<OrdersEntity> = ordersRepository.findAll()

        return query.list<OrdersEntity>()
            .flatMap { entityList: List<OrdersEntity> -> // This returns a list of orders
                // 1. Product IDs are collected here
                val allProductIds: Set<Long> = entityList
                    .flatMap { it.orderProducts }
                    .toSet() // Ensure we only query for each ID once

                // 2. Call the database to fetch ProductsEntities
                // This will return a full list of products
                productsRepository.findListOfProducts(allProductIds)
                    .onItem().transform { productsList: List<ProductsEntity> ->
                        // 3. Create a map for fast lookup
                        // productsMap: Map<Long, ProductsDto>
                        val productsMap: Map<Long, ProductsDto> = productsList
                            .associateBy { it.id!! } // This creates a map where key=ID, value=Entity
                            .mapValues { (_, entity) -> productsMapper.entityToDto(entity) } // Map to DTOs

                        // 4. Map the OrdersEntity list to OrdersDto list
                        entityList.map { orderEntity ->
                            val orderDto = ordersMapper.entityToDto(orderEntity) // This maps each order
                            val productDtos = orderEntity.orderProducts // This finds and sets the corresponding product DTO
                                .mapNotNull { productId -> productsMap[productId] }

                            orderDto.orderProducts = productDtos

                            orderDto
                        }
                    }
            }
    }

    /**
     * Function: findAllOrdersByOwner
     * What does it do: The function returns all orders issued by a specific user
     * It also returns the data from each product in the list (ProductsDTOs)
     */
    fun findAllOrdersByOwner(orderOwner: Long): Uni<List<OrdersDto>> {

        return ordersRepository.findByOwner(orderOwner)
            .flatMap { entityList: List<OrdersEntity> ->
                // Gather all Product IDs
                val allProductsIds: Set<Long> = entityList
                    .flatMap { it.orderProducts }
                    .toSet()

                // Look for entities
                productsRepository.findListOfProducts(allProductsIds)
                    .onItem().transform { productsList: List<ProductsEntity> ->
                        // Create a map
                        val productsMap: Map<Long, ProductsDto> = productsList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> productsMapper.entityToDto(entity) }

                        // Map the OrdersEntity to the enriched OrdersDto list
                        entityList.map { orderEntity ->
                            // Map the order
                            val orderDto = ordersMapper.entityToDto(orderEntity)

                            // Find the corresponding product DTOs
                            val productDtos = orderEntity.orderProducts
                                .mapNotNull { productId -> productsMap[productId] }

                            orderDto.orderProducts = productDtos
                            orderDto
                        }
                    }
            }
    }

    /**
     * Function: findOrdersByDate
     * What does it do: it looks for all orders issued either before, after, or between given dates
     */
    fun findOrdersByDate(startDate: OffsetDateTime?, endDate: OffsetDateTime?): Uni<List<OrdersDto>> {
        return ordersRepository.findByDates(startDate, endDate)
            .flatMap { entityList: List<OrdersEntity> ->
                // Gather all Product IDs
                val allProductsIds: Set<Long> = entityList
                    .flatMap { it.orderProducts }
                    .toSet()

                // Look for entities
                productsRepository.findListOfProducts(allProductsIds)
                    .onItem().transform { productsList: List<ProductsEntity> ->
                        // Create a map
                        val productsMap: Map<Long, ProductsDto> = productsList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> productsMapper.entityToDto(entity) }

                        // Map the OrdersEntity to the enriched OrdersDto list
                        entityList.map { orderEntity ->
                            // Map the order
                            val orderDto = ordersMapper.entityToDto(orderEntity)

                            // Find the corresponding product DTOs
                            val productDtos = orderEntity.orderProducts
                                .mapNotNull { productId -> productsMap[productId] }

                            orderDto.orderProducts = productDtos
                            orderDto
                        }
                    }
            }
    }

    /**
     * Function: findOrdersByDateAndOwner
     * What does it do: it looks for all orders issued either before, after, or between given dates belonging to one owner
     */
    fun findOrdersByDateAndOwner(orderOwner: Long, startDate: OffsetDateTime?, endDate: OffsetDateTime?): Uni<List<OrdersDto>> {
        return ordersRepository.findByDatesAndOwner(orderOwner, startDate, endDate)
            .flatMap { entityList: List<OrdersEntity> ->
                // Gather all Product IDs
                val allProductsIds: Set<Long> = entityList
                    .flatMap { it.orderProducts }
                    .toSet()

                // Look for entities
                productsRepository.findListOfProducts(allProductsIds)
                    .onItem().transform { productsList: List<ProductsEntity> ->
                        // Create a map
                        val productsMap: Map<Long, ProductsDto> = productsList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> productsMapper.entityToDto(entity) }

                        // Map the OrdersEntity to the enriched OrdersDto list
                        entityList.map { orderEntity ->
                            // Map the order
                            val orderDto = ordersMapper.entityToDto(orderEntity)

                            // Find the corresponding product DTOs
                            val productDtos = orderEntity.orderProducts
                                .mapNotNull { productId -> productsMap[productId] }

                            orderDto.orderProducts = productDtos
                            orderDto
                        }
                    }
            }
    }
}