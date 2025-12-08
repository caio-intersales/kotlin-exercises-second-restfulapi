package de.intersales.quickstep.products.repository

import de.intersales.quickstep.products.entity.ProductsEntity
import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Multi
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
     * Other functions suggestions:
     *
     * findByPriceRange (find a product that has a price between two given values)
     * ...
     */
}