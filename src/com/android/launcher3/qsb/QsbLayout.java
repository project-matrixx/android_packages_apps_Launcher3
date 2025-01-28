package com.android.launcher3.qsb;

import static android.view.View.MeasureSpec.EXACTLY;
import static com.android.launcher3.icons.IconNormalizer.ICON_VISIBLE_AREA_FACTOR;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.core.view.ViewCompat;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.qsb.QsbContainerView;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;
import android.view.View;

public class QsbLayout extends FrameLayout {

    private ImageView micIcon;
    private ImageView gIcon;
    private ImageView lensIcon;
    private Context mContext;
    private FrameLayout inner;

    public QsbLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public QsbLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        micIcon = findViewById(R.id.mic_icon);
        gIcon = findViewById(R.id.g_icon);
        lensIcon = findViewById(R.id.lens_icon);
        inner = findViewById(R.id.inner);

        setUpMainSearch();
        setUpBackground();
        clipIconRipples();

        boolean isThemed = Utilities.isThemedIconsEnabled(mContext);
        boolean isMusicSearchEnabled = Utilities.isMusicSearchEnabled(mContext);

        micIcon.setImageResource(isThemed ? (isMusicSearchEnabled ? R.drawable.ic_music_themed : R.drawable.ic_mic_themed) : (isMusicSearchEnabled ? R.drawable.ic_music_color : R.drawable.ic_mic_color));
        gIcon.setImageResource(isThemed ? R.drawable.ic_super_g_themed : R.drawable.ic_super_g_color);
        lensIcon.setImageResource(isThemed ? R.drawable.ic_lens_themed : R.drawable.ic_lens_color);

        setupGIcon();
        setupLensIcon();
    }

    private void clipIconRipples() {
        float cornerRadius = getCornerRadius();
        PaintDrawable pd = new PaintDrawable(Color.TRANSPARENT);
        pd.setCornerRadius(cornerRadius);
        micIcon.setClipToOutline(cornerRadius > 0);
        micIcon.setBackground(pd);
        lensIcon.setClipToOutline(cornerRadius > 0);
        lensIcon.setBackground(pd);
        gIcon.setClipToOutline(cornerRadius > 0);
        gIcon.setBackground(pd);
    }

    private void setUpBackground() {
        float cornerRadius = getCornerRadius();
        int alphaValue = (Utilities.getHotseatQsbOpacity(mContext) * 255) / 100;
        int baseColor = Themes.getAttrColor(mContext, R.attr.qsbFillColor);
        if (Utilities.isThemedIconsEnabled(mContext))
            baseColor = Themes.getAttrColor(mContext, R.attr.qsbFillColorThemed);
        int color = Color.argb(alphaValue, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
        float strokeWidth = Utilities.getHotseatQsbStrokeWidth(mContext);

        PaintDrawable backgroundDrawable = new PaintDrawable(color);
        backgroundDrawable.setCornerRadius(cornerRadius);

        if (strokeWidth != 0f) {
            PaintDrawable strokeDrawable = new PaintDrawable(Themes.getColorAccent(mContext));
            strokeDrawable.getPaint().setStyle(Paint.Style.STROKE);
            strokeDrawable.getPaint().setStrokeWidth(strokeWidth);
            strokeDrawable.setCornerRadius(cornerRadius);
            LayerDrawable combinedDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, strokeDrawable});

            inner.setClipToOutline(cornerRadius > 0);
            inner.setBackground(combinedDrawable);
        } else {
            inner.setClipToOutline(cornerRadius > 0);
            inner.setBackground(backgroundDrawable);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int requestedWidth = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        DeviceProfile dp = ActivityContext.lookupContext(mContext).getDeviceProfile();
        int cellWidth = DeviceProfile.calculateCellWidth(requestedWidth, dp.cellLayoutBorderSpacePx.x, dp.numShownHotseatIcons);
        int iconSize = (int)(Math.round((dp.iconSizePx * 0.92f)));
        int widthReduction = cellWidth - iconSize;
        int width = requestedWidth - widthReduction;
        setMeasuredDimension(width, height);

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child != null) {
                measureChildWithMargins(child, widthMeasureSpec, widthReduction, heightMeasureSpec, 0);
            }
        }
    }

    private void setUpMainSearch() {
        Intent pixelSearchIntent = mContext.getPackageManager().getLaunchIntentForPackage("rk.android.app.pixelsearch");
        setOnClickListener(view -> {
            if (pixelSearchIntent != null) {
                pixelSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mContext.startActivity(pixelSearchIntent);
            } else {
                String searchPackage = QsbContainerView.getSearchWidgetPackageName(mContext);
                Intent searchIntent = new Intent("android.search.action.GLOBAL_SEARCH")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .setPackage(searchPackage);
                mContext.startActivity(searchIntent);
            }
        });
    }

    private void setupGIcon() {
        try {
            Intent pixelSearchIntent = mContext.getPackageManager().getLaunchIntentForPackage("rk.android.app.pixelsearch");
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(Utilities.GSA_PACKAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            gIcon.setOnClickListener(view -> {
                mContext.startActivity(pixelSearchIntent != null ? pixelSearchIntent : intent);
            });
        } catch (Exception e) {
            // Do nothing
        }
    }

    private void setupLensIcon() {
        try {
            lensIcon.setOnClickListener(view -> {
                Intent lensIntent = new Intent();
                lensIntent.setAction(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData(Uri.parse(Utilities.LENS_URI))
                        .putExtra("LensHomescreenShortcut", true);
                mContext.startActivity(lensIntent);
            });
        } catch (Exception e) {
            lensIcon.setVisibility(View.GONE);
        }
    }

    private float getCornerRadius() {
        Resources res = mContext.getResources();
        float qsbWidgetHeight = res.getDimension(R.dimen.qsb_widget_height);
        float qsbWidgetPadding = res.getDimension(R.dimen.qsb_widget_vertical_padding);
        float innerHeight = qsbWidgetHeight - 2 * qsbWidgetPadding;
        return (innerHeight / 2) * ((float)Utilities.getCornerRadius(mContext) / 100f);
    }
}
