package de.intersales.quickstep.products.repository

import de.intersales.quickstep.products.entity.ProductsEntity
import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ProductsRepository : PanacheRepository<ProductsEntity> {

    /**
     * Function: findByType
     * What does it do: Allows for searching products by their type
     */
    fun findByType(type: Int): Multi<ProductsEntity?> {
        return find("type = :type", Parameters.with("type", type))
            .stream<ProductsEntity>()
    }

    /**
     * Function: findListByIds
     * What does it do: The function retrieves a list of products based on a list of IDs provided
     */
    fun findListOfProducts(ids: Collection<Long>): Uni<List<ProductsEntity>> {
        return find("id in :ids", Parameters.with("ids", ids)).list()
    }

    /**
     * Other functions suggestions:
     *
     * findByPriceRange (find a product that has a price between two given values)
     * ...
     */
}