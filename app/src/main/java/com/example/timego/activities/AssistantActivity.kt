package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
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

class AssistantActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton

    private val messages = mutableListOf<Message>()
    private lateinit var messagesAdapter: MessagesAdapter

    private var conversationId: String? = null
    private var currentContext = mutableMapOf<String, Any>()

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
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(messages) { route ->
            // Открыть детали маршрута
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
                // Создаем или загружаем диалог
                val result = repository.getOrCreateConversation(userId)
                result.onSuccess { convId ->
                    conversationId = convId
                    loadConversationHistory()

                    // Если это новый диалог, отправляем приветствие
                    if (messages.isEmpty()) {
                        addBotMessage("Привет! Как я могу тебе помочь?")
                    }
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
                repository.getConversationMessages(convId, 50).onSuccess { loadedMessages ->
                    messages.clear()
                    messages.addAll(loadedMessages)
                    messagesAdapter.notifyDataSetChanged()
                    scrollToBottom()
                }
            }
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val userId = repository.getCurrentUser()?.uid ?: return
        val convId = conversationId ?: return

        // Добавляем сообщение пользователя
        addUserMessage(text)
        etMessage.text.clear()

        // Сохраняем сообщение в Firebase
        lifecycleScope.launch {
            repository.sendMessage(convId, userId, text, "user").onSuccess {
                // Обрабатываем запрос и генерируем ответ
                processUserMessage(text)
            }.onFailure { error ->
                Log.e(TAG, "Ошибка отправки сообщения", error)
                Toast.makeText(this@AssistantActivity, "Ошибка отправки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processUserMessage(text: String) {
        val userId = repository.getCurrentUser()?.uid ?: return
        val convId = conversationId ?: return

        lifecycleScope.launch {
            try {
                // Определяем намерение пользователя
                val intent = detectIntent(text)

                when (intent) {
                    "route_search" -> {
                        // Поиск маршрутов
                        val category = extractCategory(text)
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
                    else -> {
                        val response = "Извини, я не совсем понял. Попробуй спросить о маршрутах, например: 'Покажи маршруты на природе' или 'Хочу активный отдых'"
                        addBotMessage(response)
                        repository.sendMessage(convId, null, response, "bot")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обработки сообщения", e)
            }
        }
    }

    private fun detectIntent(text: String): String {
        val lowerText = text.lowercase().trim()

        // Приветствия
        if (lowerText.matches(Regex(".*(привет|здравствуй|добрый день|добрый вечер|доброе утро|хай|hello).*"))) {
            return "greeting"
        }

        // Помощь
        if (lowerText.matches(Regex(".*(помощ|помог|как работа|что умее|что можешь|что ты|справка).*"))) {
            return "help"
        }

        // Благодарность
        if (lowerText.matches(Regex(".*(спасибо|благодар|thanks|thx).*"))) {
            return "thanks"
        }

        // Любой запрос, связанный с маршрутами, местами, отдыхом
        if (lowerText.matches(Regex(".*(маршрут|место|поездк|путешеств|отдых|отдохн|съезд|сходи|пойти|поеха|провести|времяпрепровожд|развлече|досуг|выходн|покажи|найди|посоветуй|порекоменду|хочу|хотел|интересн|куда|где|что посетить|что посмотреть|идеи|варианты|предложи).*"))) {
            return "route_search"
        }

        return "unknown"
    }

    private fun extractCategory(text: String): String? {
        val lowerText = text.lowercase().trim()

        // Природа
        if (lowerText.matches(Regex(".*(природ|лес|парк|река|озеро|водоем|водохранилищ|гор|холм|поле|луг|дерев|растени|цвет|животн|птиц|зелен|свежий воздух|на свежем|эко|экологич|пикник|костер|палатк|поход|треккинг).*"))) {
            return "nature"
        }

        // История и наследие
        if (lowerText.matches(Regex(".*(истор|музей|усадьб|дворе|замок|крепость|храм|церков|собор|монастыр|памятник|архитектур|старин|древн|наследи|культурн|экскурси|достопримечат).*"))) {
            return "history"
        }

        // Активный отдых
        if (lowerText.matches(Regex(".*(активн|спорт|велосипед|вело|бег|пробежк|катан|лыж|скейт|ролик|паркур|скалолаз|альпинизм|рафтинг|байдарк|каяк|сплав|верев|экстрим|адреналин|тренировк|физическ|движени|энергичн|подвижн).*"))) {
            return "active"
        }

        // Гастрономия
        if (lowerText.matches(Regex(".*(гастроном|еда|еды|кухн|рестор|кафе|кофейн|пекарн|кондитерск|винодел|дегустаци|продукт|блюд|вкусн|поесть|перекус|кулинар|гурман|фуд).*"))) {
            return "gastronomy"
        }

        // Семейный отдых
        if (lowerText.matches(Regex(".*(семь|семейн|дет|ребен|малыш|детск|с детьми|для детей|ребят|игров|развлекательн|аттракцион|зоопарк|аквапарк|парк развлечени).*"))) {
            return "family"
        }

        // Этнография и традиции
        if (lowerText.matches(Regex(".*(этно|традиц|культур|народн|ремесл|промысл|фольклор|обря|обыча|националь|деревенск|сельск|аутентичн|самобытн).*"))) {
            return "ethnic"
        }

        return null
    }

    private suspend fun searchRoutes(category: String?, userId: String, convId: String) {
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
                // Обновляем последнее сообщение
                if (messages.isNotEmpty() && messages.last().type == "bot") {
                    messages[messages.lastIndex] = messages.last().copy(text = response)
                    messagesAdapter.notifyItemChanged(messages.lastIndex)
                }
                repository.sendMessage(convId, null, response, "bot")
            } else {
                val response = "Конечно! Вот варианты:"
                // Обновляем последнее сообщение с маршрутами
                if (messages.isNotEmpty() && messages.last().type == "bot") {
                    messages[messages.lastIndex] = messages.last().copy(
                        text = response,
                        routes = routes
                    )
                    messagesAdapter.notifyItemChanged(messages.lastIndex)
                }

                // Сохраняем в Firebase
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
        messagesAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            rvMessages.smoothScrollToPosition(messages.size - 1)
        }
    }
}