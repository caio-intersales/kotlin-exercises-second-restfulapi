package de.intersales.quickstep.orders.service

import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.dto.ReceiveDatesDto
import de.intersales.quickstep.orders.dto.UpdateOrderDto
import de.intersales.quickstep.orders.entity.OrdersEntity
import de.intersales.quickstep.orders.mapper.OrdersMapper
import de.intersales.quickstep.orders.repository.OrdersRepository
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.entity.ProductsEntity
import de.intersales.quickstep.products.mapper.ProductsMapper
import de.intersales.quickstep.products.repository.ProductsRepository
import de.intersales.quickstep.users.dto.UsersDto
import de.intersales.quickstep.users.entity.UsersEntity
import de.intersales.quickstep.users.mapper.UsersMapper
import de.intersales.quickstep.users.repository.UsersRepository
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.smallrye.mutiny.Uni
import java.time.OffsetDateTime
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class OrdersService (
    private val ordersRepository: OrdersRepository,
    private val ordersMapper: OrdersMapper,
    private val productsRepository: ProductsRepository,
    private val productsMapper: ProductsMapper,
    private val usersRepository: UsersRepository,
    private val usersMapper: UsersMapper
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
                // 1. Start fetching the needed information to be retrieved from the db
                val allProductIds: Set<Long> = entityList
                    .flatMap { it.orderProducts }
                    .toSet() // Ensure we only query for each ID once

                val allOwnersIds: Set<Long?> = entityList
                    .map { it.orderOwner }
                    .toSet()

                // 2. Fetch information concurrently
                val productsUni: Uni<List<ProductsEntity>> = productsRepository.findListOfProducts(allProductIds)
                val ownersUni: Uni<List<UsersEntity>> = usersRepository.findListOfUsers(allOwnersIds)

                // 3. Wait for both lists to be ready
                return@flatMap Uni.combine().all().unis(productsUni, ownersUni).asTuple()
                    .onItem().transform { tuple ->
                        val productsList = tuple.item1
                        val ownersList = tuple.item2

                        // 4. Create lookup maps
                        val productsMap: Map<Long, ProductsDto> = productsList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> productsMapper.entityToDto(entity) }
                        val ownersMap: Map<Long, UsersDto> = ownersList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> usersMapper.entityToDto(entity) }

                        // 5. Map the OrdersEntity list to OrdersDto list
                        entityList.map { orderEntity ->
                            val orderDto = ordersMapper.entityToDto(orderEntity)

                            // Set products
                            val productDtos = orderEntity.orderProducts
                                .mapNotNull { productId -> productsMap[productId] }
                            orderDto.orderProducts = productDtos

                            // Set owner
                            val ownerDto = ownersMap[orderEntity.orderOwner]
                            orderDto.orderOwner = ownerDto

                            orderDto
                        }
                    }
            }
    }

    /**
     * Function: findOneOrder
     * What does it do: The function returns a specific order based on a provided ID
     */
    fun findOneOrder(id: Long): Uni<OrdersDto> {
        return ordersRepository.findById(id)
            .flatMap { orderEntity: OrdersEntity? ->
                // Handle the case in which the order doesn't exist
                if (orderEntity == null) {
                    return@flatMap Uni.createFrom().nullItem()
                }

                // Gather product IDs
                val allProductsIds: Set<Long> = orderEntity.orderProducts.toSet()

                // Owner ID
                val ownerId: Long? = orderEntity.orderOwner

                // Fetch entities concurrently
                val productsUni: Uni<List<ProductsEntity>> =
                    productsRepository.findListOfProducts(allProductsIds)

                val ownerUni: Uni<List<UsersEntity>> =
                    usersRepository.findListOfUsers(setOf(ownerId))

                return@flatMap Uni.combine().all().unis(productsUni, ownerUni).asTuple()
                    .onItem().transform { tuple ->
                        val productsList = tuple.item1
                        val ownersList = tuple.item2

                        // Build lookup maps
                        val productsMap: Map<Long, ProductsDto> = productsList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> productsMapper.entityToDto(entity) }

                        val ownersMap: Map<Long, UsersDto> = ownersList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> usersMapper.entityToDto(entity) }

                        // Build the final DTO
                        val orderDto = ordersMapper.entityToDto(orderEntity)

                        // Set products
                        val productDtos = orderEntity.orderProducts
                            .mapNotNull { productId -> productsMap[productId] }
                        orderDto.orderProducts = productDtos

                        // Set owner (may be null)
                        orderDto.orderOwner = ownersMap[ownerId]

                        orderDto
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

                if (entityList.isEmpty()) {
                    return@flatMap Uni.createFrom().item(emptyList<OrdersDto>())
                }

                // Gather all Product IDs
                val allProductsIds: Set<Long> = entityList
                    .flatMap { it.orderProducts }
                    .toSet()

                // Owners: all returned orders share the same owner → but we support flexibility
                val allOwnersIds: Set<Long?> = entityList
                    .map { it.orderOwner }
                    .toSet()

                // Fetch entities concurrently
                val productsUni = productsRepository.findListOfProducts(allProductsIds)
                val ownersUni = usersRepository.findListOfUsers(allOwnersIds)

                Uni.combine().all().unis(productsUni, ownersUni).asTuple()
                    .onItem().transform { tuple ->

                        val productsList = tuple.item1
                        val ownersList = tuple.item2

                        // Create lookup maps
                        val productsMap: Map<Long, ProductsDto> = productsList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> productsMapper.entityToDto(entity) }

                        val ownersMap: Map<Long, UsersDto> = ownersList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> usersMapper.entityToDto(entity) }

                        // Map to enriched DTOs
                        entityList.map { orderEntity ->

                            val orderDto = ordersMapper.entityToDto(orderEntity)

                            // Set product DTO list
                            val productDtos = orderEntity.orderProducts
                                .mapNotNull { productId -> productsMap[productId] }
                            orderDto.orderProducts = productDtos

                            // Set owner DTO
                            orderDto.orderOwner = ownersMap[orderEntity.orderOwner]

                            orderDto
                        }
                    }
            }
    }

    /**
     * Function: findOrdersByDate
     * What does it do: it looks for all orders issued either before, after, or between given dates; can also look for data from a specific owner (if empty, look for all orders)
     */
    fun findOrdersByDate(datesDto: ReceiveDatesDto, owner: Long? = null): Uni<List<OrdersDto>> {

        // Extract startDate and endDate from the DTO
        val startDate = datesDto.startDate
        val endDate = datesDto.endDate

        return ordersRepository.findByDates(owner, startDate, endDate)
            .flatMap { entityList: List<OrdersEntity> ->

                if (entityList.isEmpty()) {
                    return@flatMap Uni.createFrom().item(emptyList<OrdersDto>())
                }

                // 1. Gather all Product IDs
                val allProductsIds: Set<Long> = entityList
                    .flatMap { it.orderProducts }
                    .toSet()

                // 2. Gather all Owner IDs
                val allOwnersIds: Set<Long?> = entityList
                    .map { it.orderOwner }
                    .toSet()

                // 3. Fetch concurrently
                val productsUni = productsRepository.findListOfProducts(allProductsIds)
                val ownersUni = usersRepository.findListOfUsers(allOwnersIds)

                Uni.combine().all().unis(productsUni, ownersUni).asTuple()
                    .onItem().transform { tuple ->

                        val productsList = tuple.item1
                        val ownersList = tuple.item2

                        // Create product lookup map
                        val productsMap: Map<Long, ProductsDto> = productsList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> productsMapper.entityToDto(entity) }

                        // Create owner lookup map
                        val ownersMap: Map<Long, UsersDto> = ownersList
                            .associateBy { it.id!! }
                            .mapValues { (_, entity) -> usersMapper.entityToDto(entity) }

                        // Map to enriched OrdersDto
                        entityList.map { orderEntity ->

                            val orderDto = ordersMapper.entityToDto(orderEntity)

                            // Set products
                            val productDtos = orderEntity.orderProducts
                                .mapNotNull { productId -> productsMap[productId] }
                            orderDto.orderProducts = productDtos

                            // Set owner
                            orderDto.orderOwner = ownersMap[orderEntity.orderOwner]

                            orderDto
                        }
                    }
            }
    }

    /**
     * Function: updateOneOrder
     * What does it do: It updates an order based on its ID
     */
    fun updateOneOrder(dto: UpdateOrderDto): Uni<OrdersDto> {
        // Check whether the order exists
        return ordersRepository.findById(dto.id)
            .onItem().transformToUni { existingOrder ->
                // If the order is not found, throw an error
                if(existingOrder == null){
                    return@transformToUni Uni.createFrom().failure(
                        ElementNotFoundException("Order with the ID $dto.id not found")
                    )
                }

                ordersMapper.updateDataToEntity(existingOrder,dto)

                return@transformToUni Uni.createFrom().item(ordersMapper.entityToDto(existingOrder))
            }
    }

    /**
     * Function: deleteOrder
     * What does it do: It deletes an order based on its ID
     */
    fun deleteOrder(id: Long): Uni<Boolean> {
        return ordersRepository.deleteById(id)
            .onItem().transform { success ->
                if(!success){
                    throw ElementNotFoundException("Order with ID $id not found.")
                }
                true
            }
    }
}