@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalUnitApi::class)

package com.example.chat_feature.screens.chat

import android.app.Activity
import android.app.LocalActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.chat_feature.R
import com.example.chat_feature.data.experts.Expert
import com.example.chat_feature.data.experts.ExpertListRequest
import com.example.chat_feature.interfaces.HomeActivityCaller
import com.example.chat_feature.navigation.AppScreen
import com.example.chat_feature.screens.chat.components.CardErrorMessage
import com.example.chat_feature.screens.chat.components.ErrorMessage
import com.example.chat_feature.screens.chat.components.telegram.ChatItem
import com.example.chat_feature.utils.Constants
import com.example.chat_feature.utils.Resource
import com.example.chat_feature.view_models.ExpertListViewModel
import kotlinx.coroutines.launch


private const val TAG = "ExpertList"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsList(
    navController: NavController,
    viewModel: ExpertListViewModel = hiltViewModel(),
) {
    val experts = viewModel.experts
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current as? Activity
    val userId = viewModel.userId




    //TODO call init function
    LaunchedEffect(Unit) {
        viewModel.loadExpertList(
            ExpertListRequest(
                senderId = userId,
            )
        )
    }

    Scaffold(topBar =
    {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Solution Point",
                    fontSize = TextUnit(20f, TextUnitType.Sp),
                    fontWeight = FontWeight.SemiBold,

                    )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(
                    0xFFF8F8F8
                )
            ),
            navigationIcon = {
                IconButton(onClick = {

                    context?.finish()
                }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            modifier = Modifier.shadow(
                10.dp,
                shape = RoundedCornerShape(0.dp).copy(
                    bottomStart = CornerSize(15.dp),
                    bottomEnd = CornerSize(15.dp)
                ),

                )
        )
    }, content = { padding ->
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.aligned(Alignment.CenterVertically)
        ) {
            BotCard(navController)

            experts.value.let {
                when (it) {
                    is Resource.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colors.onPrimary)
                            }
                        }
                    }

                    is Resource.Failure -> {
                        Log.i(TAG, "ChatScreen: ${it.errorCode}")
                        if(it.isNetworkError == true){
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Oh! it seems you are not connected with internet",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D71D4),
                                    fontSize = TextUnit(16f, TextUnitType.Sp),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                        else{
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Internal Server Error. Please try after sometime",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D71D4),
                                    fontSize = TextUnit(16f, TextUnitType.Sp),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                    }

                    is Resource.Success -> {
                        val data = it.value
                        if (data.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Start chat with Codey",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D71D4),
                                    fontSize = TextUnit(16f, TextUnitType.Sp),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        } else {
                            UserList(data) { user ->
                                scope.launch {
                                    navController.navigate(
                                        AppScreen.ExpertChathistoryViaLoadRoom.buildRoute(
                                            senderId = userId,
                                            // receiverId = user.sentTo,
                                            queryId = user.queryId
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }


        }
    })
}


@Composable
fun UserList(users: List<Expert>, onClick: (user: Expert) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(Modifier.weight(1f)) {
            items(users) { user ->
                /*UserCard(user, onClick)
                Divider(
                    color = Color(0xFFC8C8C8),
                    thickness = 1.dp,
                    modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 0.dp)
                )*/

                ChatItem(user = user, onClick)
                Divider(
                    color = Color(0xFFE2E2E2),
                    thickness = 1.dp,
                    modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 0.dp)
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalUnitApi::class)
@Composable
fun BotCard(navController: NavController) {
    val context = LocalContext.current
    Row(modifier = Modifier.padding(15.dp)) {
        Card(
            shape = RoundedCornerShape(50),
            backgroundColor = Color(0xFF7630F2),
            modifier = Modifier
                .fillMaxWidth()
                .height(57.dp)
                .weight(1f),
            onClick = {
                navController.navigate(
                    AppScreen.ChatBot.buildRoute(
                        senderId = Constants.USER_ID,
                        receiverId = Constants.BOT_ID
                    )
                )
                Toast.makeText(
                    context,
                    "Clicked on Bot",
                    Toast.LENGTH_LONG
                ).show()
            }) {

            Box() {
                Card(
                    shape = CircleShape, backgroundColor = Color(0xFFF8F8F8),
                    modifier = Modifier.size(57.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.robot),
                        contentDescription = "Bot Picture Holder",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .width(20.dp)
                            .height(35.dp)
                            .padding(5.dp, 0.dp, 0.dp, 0.dp),
                        colorFilter = ColorFilter.tint(color = Color(0xFF7630F2))
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(65.dp, 0.dp, 0.dp, 0.dp)
                    .size(330.dp, 57.dp),
                verticalArrangement = Arrangement.aligned(Alignment.CenterVertically)
            ) {
                Text(
                    text = "Selfbest Bot",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
                Text(
                    text = "Click here to start a new query",
                    color = Color(0xFFFFFFFF),
                    fontStyle = FontStyle.Normal,
                    fontSize = TextUnit(13f, TextUnitType.Sp),
                    style = MaterialTheme.typography.body2
                )
            }

        }

        Box(modifier = Modifier.padding(10.dp, 0.dp, 5.dp, 0.dp)) {
            Card(
                shape = CircleShape, backgroundColor = Color(0xFFF8F8F8),
                modifier = Modifier.size(57.dp)
            ) {
                IconButton(onClick = {
                    Toast.makeText(context, "Clicked on Search bar", Toast.LENGTH_LONG).show()
                }) {
                    Icon(
                        Icons.TwoTone.Search,
                        contentDescription = "content description",
                        tint = Color(0XFFC8C8C8),
                        modifier = Modifier.padding(15.dp)
                    )

                }
            }
        }

    }
}


@OptIn(ExperimentalMaterialApi::class, ExperimentalUnitApi::class)
@Composable
fun UserCard(user: Expert, onClick: (user: Expert) -> Unit) {
    val context = LocalContext.current
    val iconSize = 14.dp
    val offsetInPx = LocalDensity.current.run { (iconSize / 2).roundToPx() }
    Card(onClick = {
        Toast.makeText(context, "clicked on ${user.queryId}", Toast.LENGTH_LONG).show()
        onClick.invoke(user)
    }, elevation = 0.dp)
    {
        Row(modifier = Modifier.padding(15.dp, 10.dp, 15.dp, 10.dp)) {
            Box {
                Card(
                    shape = CircleShape,
                    backgroundColor = Color(0xFFF8F8F8),
                    modifier = Modifier.size(50.dp)
                ) {

                }
                if (user.status) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = "",
                        tint = Color(0xFF2FD765),
                        modifier = Modifier
                            .offset {
                                IntOffset(x = +offsetInPx - 10, y = -offsetInPx + 10)
                            }
                            .clip(CircleShape)
                            .background(Color.White)
                            .size(iconSize)
                            .align(Alignment.BottomEnd)
                    )
                }


            }

            Column(
                modifier = Modifier
                    .padding(5.dp, 0.dp, 0.dp, 0.dp)
                    .width(240.dp),
                verticalArrangement = Arrangement.aligned(Alignment.CenterVertically)
            ) {
                Text(
                    text = user.fullName,
                    fontWeight = FontWeight.Normal,
                    //   color = selfBestDefaultColor
                    fontSize = TextUnit(12f, TextUnitType.Sp)
                )
                Spacer(modifier = Modifier.size(7.dp))
                Text(
                    text = user.queryText,
                    //color = selfBestDefaultColor,
                    fontStyle = FontStyle.Normal,
                    fontSize = TextUnit(12f, TextUnitType.Sp)
                )


            }


            Column(
                modifier = Modifier
                    .padding(5.dp, 0.dp, 0.dp, 0.dp)
            ) {
                if (user.unSeenCount == 0) {
                    Text(
                        text = "18/11/2022",
                        fontWeight = FontWeight.Normal,
                        // color = selfBestDefaultColor,
                        fontSize = TextUnit(12f, TextUnitType.Sp)
                    )
                } else {
                    Text(
                        text = "+{${user.unSeenCount}}",
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.background(Color(0xFF2FD765)),
                        color = Color.White,
                        textAlign = TextAlign.Justify,
                        // color = selfBestDefaultColor,
                        fontSize = TextUnit(12f, TextUnitType.Sp)
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                if (user.queryStatus) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Normal,
                        // color = selfBestDefaultColor,
                        fontSize = TextUnit(12f, TextUnitType.Sp)
                    )
                } else {
                    Text(
                        text = "inProgress",
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Justify,
                        // color = selfBestDefaultColor,
                        fontSize = TextUnit(12f, TextUnitType.Sp)
                    )
                }

            }

        }
    }
    /*val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.TopStart
    ) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(8.dp))
                .padding(2.dp),
            onClick = {
                Toast.makeText(context, "clicked on ${user.queryId}", Toast.LENGTH_LONG).show()
                onClick.invoke(user)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceAround


            ) {
                Card(
                    shape = CircleShape, border = BorderStroke(2.dp, color = Color.Red),
                    modifier = Modifier
                        .size(48.dp, 48.dp)
                        .padding(0.dp),
                    onClick = { Toast.makeText(context, "Profile Photo", Toast.LENGTH_LONG).show() }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = "Bot Picture Holder",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(240.dp, 50.dp),
                    verticalArrangement = Arrangement.aligned(Alignment.CenterVertically)
                ) {
                    Text(
                        text = user.fullName,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )

                    user.lastMessage?.let {
                        Text(
                            text = it.message,
                            color = Color.Red,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                Text(
                    text = "18/11/2022",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Red,

                    )

            }
        }
    }*/
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Surface(Modifier.fillMaxSize()) {
        BotCard(rememberNavController())
        //UserList()


    }
}
