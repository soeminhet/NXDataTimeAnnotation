package com.smh.nxdatetime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smh.annotation.NXDateTimeExtension
import com.smh.annotation.NXDateToString
import com.smh.annotation.NXLongToDate
import com.smh.annotation.NXLongToString
import com.smh.annotation.NXStringToDate
import com.smh.annotation.NXStringToString
import com.smh.nxdatetime.ui.theme.DateTimeAnnotationTheme
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DateTimeAnnotationTheme {
                Greeting()
            }
        }
    }
}

@Composable
fun Greeting() {
    val testOne = DateTest(dateOne = "2024-01-02", dateTwo = Date())
    val testTwo = DateTimeTest(dateOne = System.currentTimeMillis(), dateTwo = Date())

    Scaffold {
        Column(modifier = Modifier.padding(it)) {
            Text(text = "Test One", style = MaterialTheme.typography.headlineMedium)
            Text(text = "${testOne.dateOne} = ${testOne.nx_date_dateOne}", modifier = Modifier.fillMaxWidth())
            Text(text = "${testOne.dateOne} = ${testOne.nx_string_dateOne}", modifier = Modifier.fillMaxWidth())
            Text(text = "${testOne.dateTwo} = ${testOne.nx_dateTwo}", modifier = Modifier.fillMaxWidth())

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text(text = "Test Two", style = MaterialTheme.typography.headlineMedium)
            Text(text = "${testTwo.dateOne} = ${testTwo.nx_date_dateOne}", modifier = Modifier.fillMaxWidth())
            Text(text = "${testTwo.dateOne} = ${testTwo.nx_string_dateOne}", modifier = Modifier.fillMaxWidth())
            Text(text = "${testTwo.dateTwo} = ${testTwo.nx_dateTwo}", modifier = Modifier.fillMaxWidth())
        }
    }
}

@NXDateTimeExtension
data class DateTest(
    @NXStringToDate(originPattern = "yyyy-MM-dd")
    @NXStringToString(originPattern = "yyyy-MM-dd", targetPattern = "yyyy MMM dd")
    val dateOne: String,

    @NXDateToString(targetPattern = "dd MMM yyyy")
    val dateTwo: Date,
)

@NXDateTimeExtension
data class DateTimeTest(
    @NXLongToDate
    @NXLongToString(targetPattern = "yyyy MMM dd HH:mm")
    val dateOne: Long,

    @NXDateToString(targetPattern = "dd/MM/yyyy hh:mm")
    val dateTwo: Date,
)

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    DateTimeAnnotationTheme {
        Greeting()
    }
}