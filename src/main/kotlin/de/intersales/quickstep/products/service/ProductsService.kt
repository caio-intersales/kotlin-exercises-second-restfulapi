package de.intersales.quickstep.products.service

import de.intersales.quickstep.products.dto.CreateProductDto
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.dto.UpdateProductDto
import de.intersales.quickstep.products.entity.ProductsEntity
import de.intersales.quickstep.products.mapper.ProductsMapper
import de.intersales.quickstep.products.repository.ProductsRepository
import de.intersales.quickstep.exceptions.ElementNotFoundException
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ProductsService (
    private val productsRepository: ProductsRepository,
    private val productsMapper: ProductsMapper
) {

    /**
     * Function: createNewProduct
     * What does it do: the function receives data from DTO, converts it into entity data (and back), sending it to the repository to be saved to the db
     */
    fun createNewProduct(dto: CreateProductDto): Uni<ProductsDto> {
        val newEntity = productsMapper.createDataToEntity(dto)

        return productsRepository.persistAndFlush(newEntity)
            .onItem().transform {
                productsMapper.entityToDto(newEntity)
            }
    }

    /**
     * Function: findAllProducts
     * What does it do: the function reads all products from the db and returns them, transforming Entity data into DTO
     */
    fun findAllProducts(): Uni<List<ProductsDto>> {
        val query: PanacheQuery<ProductsEntity> = productsRepository.findAll()

        return query.list<ProductsEntity>()
            .onItem().transform { entityList ->
                entityList.map { entity ->
                    productsMapper.entityToDto(entity)
                }
            }
    }

    /**
     * Another form could be:
     *
     * fun findAllProducts(): Uni<List<ProductsDto>> =
     * productsRepository.findAll()
     * .list<ProductsEntity>()
     * .map { list ->
     * list.map(productsMapper::entityToDto)
     * }
     */

    /**
     * Function: findOneProduct
     * What does it do: It finds a product by its ID
     */
    fun findOneProduct(id: Long): Uni<ProductsDto> {
        return productsRepository.findById(id)
            .onItem().ifNull()
            .failWith { ElementNotFoundException("Product with ID $id not found.") }
            .map(productsMapper::entityToDto)
    }

    /**
     * Function: updateProduct
     * What does it do: It passes product data to be updated
     */
    fun updateOneProduct(dto: UpdateProductDto): Uni<ProductsDto> {
        // Check whether the product exists
        return productsRepository.findById(dto.id)
            .onItem().transformToUni { existingProduct ->
                // If the product is not found, throw an error
                if(existingProduct == null){
                    return@transformToUni Uni.createFrom().failure(
                        ElementNotFoundException("Product with ID $dto.id not found.")
                    )
                }

                // Convert DTO to Entity data
                productsMapper.updateDataToEntity(existingProduct, dto)

                // Update takes place automatically
                // Here the updated product is returned
                return@transformToUni Uni.createFrom().item(productsMapper.entityToDto(existingProduct))
            }
    }

    /**
     * Function: deleteProduct
     * What does it do: The function deletes a product by its ID
     */
    fun deleteProduct(id: Long): Uni<Boolean> {
        return productsRepository.deleteById(id)
            .onItem().transform { success ->
                if(!success){
                    throw ElementNotFoundException("Product with ID $id not found.")
                }
                true
            }
    }
}