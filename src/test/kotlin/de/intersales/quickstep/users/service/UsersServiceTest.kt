package de.intersales.quickstep.users.service

import de.intersales.quickstep.users.dto.CreateUserDto
import de.intersales.quickstep.users.dto.UpdateUserDto
import de.intersales.quickstep.users.dto.UsersDto
import de.intersales.quickstep.users.entity.UsersEntity
import de.intersales.quickstep.users.exception.DuplicateUserException
import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.users.mapper.UsersMapper
import de.intersales.quickstep.users.repository.UsersRepository
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import io.quarkus.hibernate.reactive.panache.PanacheQuery

// This is a helper function to use Mockito's any() in a safe way for non-nullable Kotlin types
// This is a common workaround for "any() must not be null" errors in Kotlin/Mockito
fun <T> anyNotNull(): T {
    // This suppresses the unchecked cast warning and returns the result of Mockito's any()
    @Suppress("UNCHECKED_CAST")
    return any<T>() as T
}

@QuarkusTest
class UsersServiceTest {

    private lateinit var usersService: UsersService

    // Mocked Dependencies
    @InjectMock
    lateinit var usersRepository: UsersRepository

    @InjectMock
    lateinit var usersMapper: UsersMapper

    private val USER_ID = 42L
    private val TEST_EMAIL = "test@email.com"
    private val RAW_PASSWORD = "rawPassword123"

    // lateinit var are set to nullable var to avoid TestInstantiationException
    private var USER_ENTITY: UsersEntity? = null
    private var USER_DTO: UsersDto? = null
    private var CREATE_DTO: CreateUserDto? = null
    private var UPDATE_DTO: UpdateUserDto? = null

    @BeforeEach
    fun setup() {
        // Reset mocks to assure stability
        reset(usersRepository, usersMapper)

        // Initialise service manually
        usersService = UsersService(usersRepository, usersMapper)

        // Initialise test data
        CREATE_DTO = CreateUserDto(
            firstName = "Jane",
            lastName = "Doe",
            email = TEST_EMAIL,
            rawPassword = RAW_PASSWORD,
            deliveryAddress = "Main St 123"
        )

        UPDATE_DTO = UpdateUserDto(
            id = USER_ID,
            firstName = "Jane",
            lastName = "Doe",
            email = TEST_EMAIL,
            deliveryAddress = "Main St 123"
        )

        USER_ENTITY = UsersEntity().apply {
            id = USER_ID
            emailAddress = TEST_EMAIL
        }

        USER_DTO = UsersDto(
            id = USER_ID,
            firstName = "Jane",
            lastName = "Doe",
            email = TEST_EMAIL,
            deliveryAddress = "Main St 123"
        )
    }


    // --- createNewUser Tests ---
    @Test
    fun `createNewUser should successfully create a new user`() {
        val userEntity = USER_ENTITY!!
        val userDto = USER_DTO!!
        val createDto = CREATE_DTO!!

        mockWhen(usersRepository.findByEmail(TEST_EMAIL)).thenReturn(Uni.createFrom().item(null as UsersEntity?))
        mockWhen(usersMapper.createDataToEntity(createDto)).thenReturn(userEntity)
        mockWhen(usersRepository.persistAndFlush(anyNotNull())).thenReturn(Uni.createFrom().item(userEntity))
        mockWhen(usersMapper.entityToDto(userEntity)).thenReturn(userDto)

        val subscriber = usersService.createNewUser(createDto)
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertCompleted().assertItem(userDto)

        verify(usersRepository).findByEmail(TEST_EMAIL)
        verify(usersRepository).persistAndFlush(anyNotNull())
        verify(usersMapper).entityToDto(userEntity)
    }

    @Test
    fun `createNewUser should throw DuplicateUserException if email exists`() {
        val userEntity = USER_ENTITY!!
        val createDto = CREATE_DTO!!

        mockWhen(usersRepository.findByEmail(TEST_EMAIL)).thenReturn(Uni.createFrom().item(userEntity))

        val subscriber = usersService.createNewUser(createDto)
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertFailedWith(DuplicateUserException::class.java)
        verify(usersRepository, never()).persistAndFlush(anyNotNull())
    }

    // --- showAllUsers Test ---
    @Test
    fun `showAllUsers should return a list of all users`() {
        val userEntity = USER_ENTITY!!
        val userDto = USER_DTO!!

        val entityList = listOf(userEntity)
        val dtoList = listOf(userDto)

        // Using explicit class type for mock creation to avoid Kotlin/Mockito ambiguities
        val mockQuery = mock(PanacheQuery::class.java) as PanacheQuery<UsersEntity>

        mockWhen(usersRepository.showAllUsers()).thenReturn(mockQuery)
        mockWhen(mockQuery.list<UsersEntity>()).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(usersMapper.entityToDto(userEntity)).thenReturn(userDto)

        val subscriber = usersService.findAllUsers()
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertCompleted().assertItem(dtoList)
    }

    // --- findOneUser Tests ---
    @Test
    fun `findOneUser should return UsersDto when user is found`() {
        val userEntity = USER_ENTITY!!
        val userDto = USER_DTO!!

        mockWhen(usersRepository.findUserById(USER_ID)).thenReturn(Uni.createFrom().item(userEntity))
        mockWhen(usersMapper.entityToDto(userEntity)).thenReturn(userDto)

        val subscriber = usersService.findOneUser(USER_ID)
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertCompleted().assertItem(userDto)
        // This test now works because the class was correctly initialized
    }

    @Test
    fun `findOneUser should fail with ElementNotFoundException when user is not found`() {
        mockWhen(usersRepository.findUserById(USER_ID)).thenReturn(Uni.createFrom().item(null as UsersEntity?))

        val subscriber = usersService.findOneUser(USER_ID)
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertFailedWith(ElementNotFoundException::class.java)
        verify(usersMapper, never()).entityToDto(anyNotNull())
    }

    // --- updateOneUser Tests ---
    @Test
    fun `updateOneUser should successfully update an existing user`() {
        val userEntity = USER_ENTITY!!
        val userDto = USER_DTO!!
        val updateDto = UPDATE_DTO!!

        mockWhen(usersRepository.findUserById(USER_ID)).thenReturn(Uni.createFrom().item(userEntity))
        doNothing().`when`(usersMapper).updateDataToEntity(userEntity, updateDto)
        mockWhen(usersMapper.entityToDto(userEntity)).thenReturn(userDto)

        val subscriber = usersService.updateOneUser(updateDto)
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertCompleted().assertItem(userDto)
        verify(usersRepository).findUserById(USER_ID)
        verify(usersMapper).updateDataToEntity(userEntity, updateDto)
        verify(usersMapper).entityToDto(userEntity)
    }

    @Test
    fun `updateOneUser should fail with ElementNotFoundException if user is not found`() {
        val updateDto = UPDATE_DTO!!

        mockWhen(usersRepository.findUserById(USER_ID)).thenReturn(Uni.createFrom().item(null as UsersEntity?))

        val subscriber = usersService.updateOneUser(updateDto)
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertFailedWith(ElementNotFoundException::class.java)

        // FIX: Using the safe anyNotNull() helper to resolve the NullPointerException
        verify(usersMapper, never()).updateDataToEntity(
            anyNotNull<UsersEntity>(),
            anyNotNull<UpdateUserDto>()
        )
    }

    // --- deleteUser Tests ---
    @Test
    fun `deleteUser should return true on successful deletion`() {
        mockWhen(usersRepository.deleteById(USER_ID)).thenReturn(Uni.createFrom().item(true))

        val subscriber = usersService.deleteUser(USER_ID)
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertCompleted().assertItem(true)
    }

    @Test
    fun `deleteUser should fail with ElementNotFoundException if user is not deleted`() {
        mockWhen(usersRepository.deleteById(USER_ID)).thenReturn(Uni.createFrom().item(false))

        val subscriber = usersService.deleteUser(USER_ID)
            .subscribe().withSubscriber(UniAssertSubscriber.create())

        subscriber.assertFailedWith(ElementNotFoundException::class.java)
    }
}