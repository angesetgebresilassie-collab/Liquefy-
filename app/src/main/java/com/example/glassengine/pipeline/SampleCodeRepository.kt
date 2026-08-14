package com.example.glassengine.pipeline

/**
 * Pre-packaged realistic Android source code samples for testing and demonstration.
 */
object SampleCodeRepository {

    val SAMPLE_CONSTRAINT_LAYOUT = """
        <?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            xmlns:tools="http://schemas.android.com/tools"
            android:id="@+id/mainConstraintLayout"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="#FFFFFF"
            android:padding="16dp">

            <TextView
                android:id="@+id/tvHeaderTitle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="System Telemetry"
                android:textColor="#111827"
                android:textSize="24sp"
                android:textStyle="bold"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent" />

            <androidx.cardview.widget.CardView
                android:id="@+id/cardMetrics"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                app:cardBackgroundColor="#FFFFFF"
                app:cardCornerRadius="12dp"
                app:cardElevation="4dp"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@id/tvHeaderTitle">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:background="#FAFAFA"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:id="@+id/tvCpuUsage"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="GPU Shader Pipeline: Active"
                        android:textColor="#374151"
                        android:textSize="16sp" />

                    <TextView
                        android:id="@+id/tvMemoryUsage"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:text="AGSL Refraction Index: 1.45"
                        android:textColor="#6B7280"
                        android:textSize="14sp" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <LinearLayout
                android:id="@+id/containerActions"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:background="#FFFFFF"
                android:orientation="horizontal"
                android:padding="8dp"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@id/cardMetrics">

                <Button
                    android:id="@+id/btnRefresh"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Calibrate Lens" />

                <Button
                    android:id="@+id/btnExport"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:layout_weight="1"
                    android:text="Export Bytecode" />
            </LinearLayout>

        </androidx.constraintlayout.widget.ConstraintLayout>
    """.trimIndent()

    val SAMPLE_LOGIN_LAYOUT = """
        <?xml version="1.0" encoding="utf-8"?>
        <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
            android:id="@+id/loginRoot"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="?android:attr/colorBackground"
            android:gravity="center"
            android:orientation="vertical"
            android:padding="24dp">

            <ImageView
                android:id="@+id/ivLogo"
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:contentDescription="App Logo"
                android:src="@mipmap/ic_launcher" />

            <TextView
                android:id="@+id/tvTitle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="Welcome Back"
                android:textSize="22sp"
                android:textStyle="bold" />

            <LinearLayout
                android:id="@+id/loginFormContainer"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="24dp"
                android:background="#FFFFFF"
                android:orientation="vertical"
                android:padding="16dp">

                <EditText
                    android:id="@+id/etEmail"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:hint="Email Address"
                    android:inputType="textEmailAddress" />

                <EditText
                    android:id="@+id/etPassword"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:hint="Password"
                    android:inputType="textPassword" />

                <Button
                    android:id="@+id/btnSignIn"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="16dp"
                    android:text="Sign In" />
            </LinearLayout>
        </LinearLayout>
    """.trimIndent()

    val SAMPLE_KOTLIN_ACTIVITY = """
        package com.example.analytics

        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity
        import com.example.analytics.databinding.ActivityMainBinding

        class MainActivity : AppCompatActivity() {

            private lateinit var binding: ActivityMainBinding

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)

                setupListeners()
                loadTelemetryData()
            }

            private fun setupListeners() {
                binding.btnRefresh.setOnClickListener {
                    loadTelemetryData()
                }
            }

            private fun loadTelemetryData() {
                // Fetch telemetries
            }
        }
    """.trimIndent()

    val SAMPLE_JAVA_ACTIVITY = """
        package com.example.legacy;

        import android.os.Bundle;
        import androidx.appcompat.app.AppCompatActivity;
        import android.view.View;
        import android.widget.Button;

        public class DashboardActivity extends AppCompatActivity {

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_dashboard);

                Button btnRefresh = findViewById(R.id.btnRefresh);
                btnRefresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refreshData();
                    }
                });
            }

            private void refreshData() {
                // Update views
            }
        }
    """.trimIndent()

    val SAMPLE_KOTLIN_FRAGMENT = """
        package com.example.fragments

        import android.os.Bundle
        import android.view.LayoutInflater
        import android.view.View
        import android.view.ViewGroup
        import androidx.fragment.app.Fragment
        import com.example.fragments.databinding.FragmentProfileBinding

        class ProfileFragment : Fragment() {

            private var _binding: FragmentProfileBinding? = null
            private val binding get() = _binding!!

            override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View {
                _binding = FragmentProfileBinding.inflate(inflater, container, false)
                return binding.root
            }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                bindUserProfile()
            }

            private fun bindUserProfile() {
                // Populate profile
            }

            override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
            }
        }
    """.trimIndent()

    val SAMPLE_THEMES_XML = """
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
            <style name="Theme.GlassmorphismDemo" parent="Theme.Material3.DayNight.NoActionBar">
                <item name="colorPrimary">#6366F1</item>
                <item name="colorSecondary">#EC4899</item>
                <item name="android:windowBackground">#F8FAFC</item>
            </style>
        </resources>
    """.trimIndent()

    val SAMPLE_MANIFEST_XML = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="com.example.analytics">

            <application
                android:allowBackup="true"
                android:icon="@mipmap/ic_launcher"
                android:label="@string/app_name"
                android:roundIcon="@mipmap/ic_launcher_round"
                android:supportsRtl="true"
                android:theme="@style/Theme.GlassmorphismDemo">
                <activity
                    android:name=".MainActivity"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
                <activity android:name=".DashboardActivity" />
            </application>

        </manifest>
    """.trimIndent()

    fun getDefaultProjectFiles(): List<ProjectSourceFile> {
        return listOf(
            ProjectSourceFile("app/src/main/res/layout/activity_main.xml", SAMPLE_CONSTRAINT_LAYOUT, FileCategory.XML_LAYOUT),
            ProjectSourceFile("app/src/main/res/layout/activity_login.xml", SAMPLE_LOGIN_LAYOUT, FileCategory.XML_LAYOUT),
            ProjectSourceFile("app/src/main/java/com/example/analytics/MainActivity.kt", SAMPLE_KOTLIN_ACTIVITY, FileCategory.KOTLIN_SOURCE),
            ProjectSourceFile("app/src/main/java/com/example/legacy/DashboardActivity.java", SAMPLE_JAVA_ACTIVITY, FileCategory.JAVA_SOURCE),
            ProjectSourceFile("app/src/main/java/com/example/fragments/ProfileFragment.kt", SAMPLE_KOTLIN_FRAGMENT, FileCategory.KOTLIN_SOURCE),
            ProjectSourceFile("app/src/main/res/values/themes.xml", SAMPLE_THEMES_XML, FileCategory.THEME_STYLES),
            ProjectSourceFile("app/src/main/AndroidManifest.xml", SAMPLE_MANIFEST_XML, FileCategory.MANIFEST)
        )
    }
}
