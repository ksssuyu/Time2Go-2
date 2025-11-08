package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timego.R
import com.example.timego.adapters.MessagesAdapter
import com.example.timego.models.Message
import com.example.timego.repository.FirebaseRepository
import kotlinx.coroutines.launch
import android.widget.ImageView

class AssistantActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: android.widget.ImageButton
    private lateinit var btnBack: ImageView

    private val messages = mutableListOf<Message>()
    private lateinit var messagesAdapter: MessagesAdapter

    private var conversationId: String? = null

    companion object {
        private const val TAG = "AssistantActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistant)

        repository = FirebaseRepository()

        initViews()
        setupRecyclerView()
        setupListeners()
        initializeConversation()
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rv_messages)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)
        btnBack = findViewById(R.id.btn_back)
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(messages) { route ->
            val intent = Intent(this, RouteDetailActivity::class.java)
            intent.putExtra(RouteDetailActivity.EXTRA_ROUTE_ID, route.routeId)
            startActivity(intent)
        }

        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messagesAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun initializeConversation() {
        val userId = repository.getCurrentUser()?.uid

        if (userId == null) {
            Toast.makeText(this, "Войдите для использования ассистента", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val result = repository.getOrCreateConversation(userId)
                result.onSuccess { convId ->
                    conversationId = convId
                    loadConversationHistory()
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка инициализации диалога", error)
                    Toast.makeText(this@AssistantActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
            }
        }
    }

    private fun loadConversationHistory() {
        conversationId?.let { convId ->
            lifecycleScope.launch {
                try {
                    repository.getConversationMessages(convId, 100).onSuccess { loadedMessages ->
                        messages.clear()
                        messages.addAll(loadedMessages)

                        if (messages.isEmpty()) {
                            val greetings = listOf(
                                "Привет! 👋 Я твой персональный помощник по путешествиям. Расскажи, какие впечатления ты хочешь получить?",
                                "Здравствуй! 🌟 Я знаю множество удивительных мест. Какое настроение у тебя сегодня?",
                                "Рад встрече! ✨ Давай найдем идеальный маршрут для твоего отдыха!"
                            )
                            val welcomeMessage = greetings.random()
                            addBotMessage(welcomeMessage)

                            // Сохраняем приветственное сообщение
                            repository.sendMessage(convId, null, welcomeMessage, "bot")
                        }

                        messagesAdapter.notifyDataSetChanged()
                        scrollToBottom()
                    }.onFailure { error ->
                        Log.e(TAG, "Ошибка загрузки истории", error)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при загрузке истории", e)
                }
            }
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = repository.getCurrentUser()?.uid
        val convId = conversationId

        if (userId == null || convId == null) {
            Toast.makeText(this, "Ошибка: не удалось отправить сообщение", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Отправка сообщения: $text")

        // Добавляем сообщение пользователя в UI СРАЗУ
        addUserMessage(text)
        etMessage.text.clear()

        // Сохраняем сообщение в Firebase и обрабатываем
        lifecycleScope.launch {
            try {
                repository.sendMessage(convId, userId, text, "user").onSuccess {
                    Log.d(TAG, "Сообщение пользователя сохранено в Firebase")
                    // Обрабатываем запрос и генерируем ответ
                    processUserMessage(text)
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка отправки сообщения в Firebase", error)
                    Toast.makeText(
                        this@AssistantActivity,
                        "Ошибка отправки: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при отправке", e)
                Toast.makeText(
                    this@AssistantActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun processUserMessage(text: String) {
        val userId = repository.getCurrentUser()?.uid ?: return
        val convId = conversationId ?: return

        lifecycleScope.launch {
            try {
                val intent = detectIntent(text)
                Log.d(TAG, "Определено намерение: $intent")

                when (intent) {
                    "route_search" -> {
                        val category = extractCategory(text)
                        if (category != null) {
                            Log.d(TAG, "Категория найдена: $category")
                        }
                        searchRoutes(category, userId, convId)
                    }
                    "greeting" -> {
                        val response = "Рад тебя видеть! Я могу помочь найти интересные маршруты. Просто скажи, что тебя интересует!"
                        addBotMessage(response)
                        repository.sendMessage(convId, null, response, "bot")
                    }
                    "help" -> {
                        val response = "Я могу помочь тебе найти маршруты по категориям: природа, история, активный отдых, гастрономия, семейные и этнография. Просто скажи, что тебя интересует!"
                        addBotMessage(response)
                        repository.sendMessage(convId, null, response, "bot")
                    }
                    "thanks" -> {
                        val responses = listOf(
                            "Пожалуйста! Рад помочь! 😊",
                            "Всегда рад! Если нужна еще помощь - обращайся!",
                            "Не за что! Приятного отдыха! 🌟"
                        )
                        val response = responses.random()
                        addBotMessage(response)
                        repository.sendMessage(convId, null, response, "bot")
                    }
                    else -> {
                        val response = "Извини, я не совсем понял. Попробуй спросить о маршрутах, например: 'Покажи маршруты на природе' или 'Хочу активный отдых'"
                        addBotMessage(response)
                        repository.sendMessage(convId, null, response, "bot")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обработки сообщения", e)
                val errorResponse = "Произошла ошибка. Попробуй еще раз!"
                addBotMessage(errorResponse)
                repository.sendMessage(convId, null, errorResponse, "bot")
            }
        }
    }

    private fun detectIntent(text: String): String {
        val lowerText = text.lowercase().trim()

        if (lowerText.matches(Regex(".*(привет|здравствуй|добрый день|добрый вечер|доброе утро|хай|hello|hi|йо|здорово).*"))) {
            return "greeting"
        }

        if (lowerText.matches(Regex(".*(помощ|помог|как работа|что умее|что можешь|что ты|справка|команды|функци).*"))) {
            return "help"
        }

        if (lowerText.matches(Regex(".*(спасибо|благодар|thanks|thx|пасиб|сенкс|круто|супер ты|молодец).*"))) {
            return "thanks"
        }

        if (lowerText.matches(Regex(".*(маршрут|место|поездк|путешеств|отдых|отдохн|съезд|сходи|пойти|поеха|провести|времяпрепровожд|развлече|досуг|выходн|покажи|найди|посоветуй|порекоменду|хочу|хотел|интересн|куда|где|что посетить|что посмотреть|идеи|варианты|предложи|скучно|нечем заняться|чем занять).*"))) {
            return "route_search"
        }

        return "unknown"
    }

    private fun extractCategory(text: String): String? {
        val lowerText = text.lowercase().trim()

        if (lowerText.matches(Regex(".*(природ|лес|парк|река|озеро|водоем|водохранилищ|гор|холм|поле|луг|дерев|растени|цвет|животн|птиц|зелен|свежий воздух|на свежем|эко|экологич|пикник|костер|палатк|поход|треккинг|тропа|тропинк|роща|бор|берег|пляж|остров).*"))) {
            return "nature"
        }

        if (lowerText.matches(Regex(".*(истор|музей|усадьб|дворе|замок|крепость|храм|церков|собор|монастыр|памятник|архитектур|старин|древн|наследи|культурн|экскурси|достопримечат|былое|прошлое|эпох|век|столет).*"))) {
            return "history"
        }

        if (lowerText.matches(Regex(".*(активн|спорт|велосипед|вело|бег|пробежк|катан|лыж|скейт|ролик|паркур|скалолаз|альпинизм|рафтинг|байдарк|каяк|сплав|верев|экстрим|адреналин|тренировк|физическ|движени|энергичн|подвижн|динамичн|бодр|заряд).*"))) {
            return "active"
        }

        if (lowerText.matches(Regex(".*(гастроном|еда|еды|кухн|рестор|кафе|кофейн|пекарн|кондитерск|винодел|дегустаци|продукт|блюд|вкусн|поесть|перекус|кулинар|гурман|фуд|деликатес|лакомств).*"))) {
            return "gastronomy"
        }

        if (lowerText.matches(Regex(".*(семь|семейн|дет|ребен|малыш|детск|с детьми|для детей|ребят|игров|развлекательн|аттракцион|зоопарк|аквапарк|парк развлечени|всей семьей|с семьей|родител).*"))) {
            return "family"
        }

        if (lowerText.matches(Regex(".*(этно|традиц|культур|народн|ремесл|промысл|фольклор|обря|обыча|националь|деревенск|сельск|аутентичн|самобытн|корни|истоки).*"))) {
            return "ethnic"
        }

        return null
    }

    private suspend fun searchRoutes(category: String?, userId: String, convId: String) {
        try {
            val result = if (category != null) {
                addBotMessage("Ищу для тебя маршруты...")
                repository.getRoutesByCategory(category, 5)
            } else {
                addBotMessage("Вот несколько интересных маршрутов:")
                repository.getPopularRoutes(5)
            }

            result.onSuccess { routes ->
                if (routes.isEmpty()) {
                    val response = "К сожалению, не нашел подходящих маршрутов. Попробуй другую категорию!"
                    if (messages.isNotEmpty() && messages.last().type == "bot") {
                        messages[messages.lastIndex] = messages.last().copy(text = response)
                        messagesAdapter.notifyItemChanged(messages.lastIndex)
                    }
                    repository.sendMessage(convId, null, response, "bot")
                } else {
                    val response = "Конечно! Вот варианты:"
                    if (messages.isNotEmpty() && messages.last().type == "bot") {
                        messages[messages.lastIndex] = messages.last().copy(
                            text = response,
                            routes = routes
                        )
                        messagesAdapter.notifyItemChanged(messages.lastIndex)
                    }

                    repository.sendMessageWithRoutes(convId, null, response, "bot", routes.map { it.routeId })
                }
                scrollToBottom()
            }.onFailure { error ->
                Log.e(TAG, "Ошибка поиска маршрутов", error)
                val response = "Произошла ошибка при поиске маршрутов. Попробуй еще раз!"
                if (messages.isNotEmpty() && messages.last().type == "bot") {
                    messages[messages.lastIndex] = messages.last().copy(text = response)
                    messagesAdapter.notifyItemChanged(messages.lastIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при поиске маршрутов", e)
        }
    }

    private fun addUserMessage(text: String) {
        val message = Message(
            messageId = "",
            conversationId = conversationId ?: "",
            userId = repository.getCurrentUser()?.uid,
            text = text,
            type = "user",
            createdAt = com.google.firebase.Timestamp.now()
        )
        messages.add(message)
        Log.d(TAG, "Сообщение пользователя добавлено в список. Всего сообщений: ${messages.size}")
        messagesAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    private fun addBotMessage(text: String) {
        val message = Message(
            messageId = "",
            conversationId = conversationId ?: "",
            userId = null,
            text = text,
            type = "bot",
            createdAt = com.google.firebase.Timestamp.now()
        )
        messages.add(message)
        Log.d(TAG, "Сообщение бота добавлено в список. Всего сообщений: ${messages.size}")
        messagesAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            Log.d(TAG, "Прокрутка к последнему сообщению (позиция ${messages.size - 1})")
            rvMessages.post {
                rvMessages.smoothScrollToPosition(messages.size - 1)
            }
        }
    }
}