package de.intersales.quickstep.addresses.service

import de.intersales.quickstep.addresses.dto.AddressesDto
import de.intersales.quickstep.addresses.dto.CreateAddressDto
import de.intersales.quickstep.addresses.dto.UpdateAddressDto
import de.intersales.quickstep.addresses.entity.AddressesEntity
import de.intersales.quickstep.addresses.mapper.AddressesMapper
import de.intersales.quickstep.addresses.repository.AddressesRepository
import de.intersales.quickstep.products.service.anyNotNull
import org.junit.jupiter.api.Assertions.*
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.*
import kotlin.Long

@QuarkusTest
class AddressesServiceTest {
    private lateinit var addressesService: AddressesService

    // Mocked dependencies
    @InjectMock
    lateinit var addressesRepository: AddressesRepository

    @InjectMock
    lateinit var addressesMapper: AddressesMapper

    private val ADDRESS_ID  = 10L
    private val USER_ID     = 11L
    private val COUNTRY_CODE        = "US"
    private val NEW_COUNTRY_CODE    = "DE"

    private var ADDRESS_ENTITY: AddressesEntity? = null
    private var ADDRESS_DTO: AddressesDto? = null
    private var CREATE_DTO: CreateAddressDto? = null
    private var UPDATE_DTO: UpdateAddressDto? = null

    @BeforeEach
    fun setup() {
        reset(addressesRepository, addressesMapper)

        // Initialise service and test data
        addressesService = AddressesService(addressesRepository, addressesMapper)

        CREATE_DTO = CreateAddressDto(
            userId       = USER_ID,
            street      = "Main St",
            houseNumber = "12",
            city        = "Springfield",
            state       = "NY",
            zip         = "123ZZ",
            country     = COUNTRY_CODE
        )
        
        UPDATE_DTO = UpdateAddressDto(
            id      = ADDRESS_ID,
            userId  = USER_ID,
            street  = "Grossstr.",
            houseNumber = "125",
            city    = "Duisburg",
            state   = "NRW",
            zip     = "47000",
            country = NEW_COUNTRY_CODE
        )

        ADDRESS_ENTITY = AddressesEntity().apply {
            id = ADDRESS_ID
        }

        ADDRESS_DTO = AddressesDto(
            id = ADDRESS_ID,
            userId = USER_ID,
            street = "Main St",
            houseNumber = "12",
            city = "Springfield",
            state = "NY",
            zip = "123ZZ",
            country = COUNTRY_CODE
        )
    }

    @Test
    fun `createNewAddress should persist and return DTO`() {
        mockWhen(addressesMapper.createDataToEntity(anyNotNull())).thenReturn(ADDRESS_ENTITY!!)

        mockWhen(addressesRepository.persistAndFlush(ADDRESS_ENTITY!!)).thenReturn(Uni.createFrom().item(ADDRESS_ENTITY!!))

        mockWhen(addressesMapper.entityToDto(ADDRESS_ENTITY!!)).thenReturn(ADDRESS_DTO!!)

        val result = addressesService.createNewAddress(CREATE_DTO!!).await().indefinitely()

        assertEquals(ADDRESS_DTO, result)
        verify(addressesRepository).persistAndFlush(ADDRESS_ENTITY!!)
        verify(addressesMapper).entityToDto(ADDRESS_ENTITY!!)
    }

    @Test
    fun `findByUser should return DTO when found`() {
        mockWhen(addressesRepository.findByUser(USER_ID))
            .thenReturn(Uni.createFrom().item(ADDRESS_ENTITY!!))

        mockWhen(addressesMapper.entityToDto(ADDRESS_ENTITY!!)).thenReturn(ADDRESS_DTO!!)

        val result = addressesService.findByUser(USER_ID).await().indefinitely()

        assertEquals(ADDRESS_DTO, result)
    }

    @Test
    fun `findByCountry should return DTO when found`() {
        val entityList = listOf(ADDRESS_ENTITY!!)

        mockWhen(addressesRepository.findByCountry(COUNTRY_CODE))
            .thenReturn(Uni.createFrom().item(entityList))

        mockWhen(addressesMapper.entityToDto(ADDRESS_ENTITY!!)).thenReturn(ADDRESS_DTO!!)

        val result = addressesService.findByCountry(COUNTRY_CODE).await().indefinitely()

        assertEquals(1, result.size)
        assertEquals(ADDRESS_DTO, result[0])
        verify(addressesMapper).entityToDto(ADDRESS_ENTITY!!)
    }
}