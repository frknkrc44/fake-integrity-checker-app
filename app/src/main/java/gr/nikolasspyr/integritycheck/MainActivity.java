package gr.nikolasspyr.integritycheck;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import gr.nikolasspyr.integritycheck.dialogs.AboutDialog;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btn;
    private ImageView deviceIntegrityIcon;
    private ImageView basicIntegrityIcon;
    private ImageView strongIntegrityIcon;
    private ImageView virtualIntegrityIcon;

    private Group virtualIntegrity;

    private String jsonResponse;
    private Integer[] integrityState = {-1, -1, -1, -1};

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(
                    findViewById(android.R.id.content),
                    (view, insets) -> {
                        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                        return insets;
                    }
            );
        }

        btn = findViewById(R.id.check_btn);

        basicIntegrityIcon = findViewById(R.id.basic_integrity_icon);
        deviceIntegrityIcon = findViewById(R.id.device_integrity_icon);
        strongIntegrityIcon = findViewById(R.id.strong_integrity_icon);
        virtualIntegrityIcon = findViewById(R.id.virtual_integrity_icon);

        virtualIntegrity = findViewById(R.id.virtual_integrity);

        btn.setOnClickListener(view -> {
            toggleButtonLoading(true);

            jsonResponse = null;
            integrityState = new Integer[]{-1, -1, -1, -1};
            setIcons(integrityState);

            executor.execute(() -> {
                try {
                    Thread.sleep(1000);
                } catch (Throwable ignore) {}

                runOnUiThread(() -> {
                    toggleButtonLoading(false);
                    jsonResponse = "{\"fakeResponse\": true}";

                    for (int i = 0; i < integrityState.length - 1; i++) {
                        integrityState[i] = 1;
                    }

                    setIcons(integrityState);
                });
            });
        });
    }

    private void toggleButtonLoading(boolean isLoading) {
        setButtonLoading(btn, isLoading);
        btn.setEnabled(!isLoading);
    }

    private Drawable getProgressBarDrawable(Context context) {
        CircularProgressIndicator drawable = new CircularProgressIndicator(context);
        drawable.setIndicatorSize(48);
        drawable.setTrackThickness(5);
        drawable.setIndicatorColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondary, Color.BLUE));
        drawable.setIndeterminate(true);
        return drawable.getIndeterminateDrawable();
    }

    private void setButtonLoading(MaterialButton button, boolean loading) {
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_START);

        if (loading) {
            Drawable drawable = button.getIcon();
            if (!(drawable instanceof Animatable)) {
                drawable = getProgressBarDrawable(button.getContext());
                if (drawable instanceof Animatable) {
                    button.setIcon(drawable);
                    ((Animatable) drawable).start();
                }
            }
        } else {
            button.setIcon(null);
        }
    }

    private void setIcons(Integer[] integrityState) {
        setIcon(basicIntegrityIcon, integrityState[0]);
        setIcon(deviceIntegrityIcon, integrityState[1]);
        setIcon(strongIntegrityIcon, integrityState[2]);
        setIcon(virtualIntegrityIcon, integrityState[3]);

        if (integrityState[3] != -1) {
            virtualIntegrity.setVisibility(View.VISIBLE);
        } else {
            virtualIntegrity.setVisibility(View.GONE);
        }
    }

    private void setIcon(ImageView img, int state) {
        if (state == -1) {
            img.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_unknown));
            img.setContentDescription(getString(R.string.status_unknown));
        } else if (state == 0) {
            img.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fail));
            img.setContentDescription(getString(R.string.status_fail));
        } else {
            img.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_pass));
            img.setContentDescription(getString(R.string.status_pass));
        }
    }

    // Menu stuff
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.about) {
            new AboutDialog(MainActivity.this).show();
            return true;
        } else if (id == R.id.json_response) {
            if (jsonResponse == null) {
                Toast.makeText(this, R.string.check_first, Toast.LENGTH_SHORT).show();
            } else {
                new MaterialAlertDialogBuilder(MainActivity.this, R.style.Theme_PlayIntegrityAPIChecker_Dialogs)
                        .setTitle(R.string.json_response)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, null)
                        .setNeutralButton(R.string.copy_json, (dialogInterface, i) -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("", jsonResponse);
                            clipboard.setPrimaryClip(clip);
                            dialogInterface.dismiss();
                            Toast.makeText(MainActivity.this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
                        })
                        .setMessage(jsonResponse)
                        .show();
            }
            return true;
        } else if (id == R.id.documentation) {
            Utils.openLink(getString(R.string.docs_link), this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }


}