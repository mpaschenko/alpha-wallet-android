package io.stormbird.wallet.ui.widget.holder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.AttributeType;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.entity.TokenScriptResult;
import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;
import io.stormbird.token.util.ZonedDateTime;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.TokenFunctionActivity;
import io.stormbird.wallet.web3.Web3TokenView;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.PageReadyCallback;
import io.stormbird.wallet.widget.ProgressView;

import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceScriptHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener, PageReadyCallback
{
    public static final int VIEW_TYPE = 1011;

    private final WebView iFrame;
    private final Web3TokenView tokenView;
    private final Token token;
    private final LinearLayout iFrameLayout;
    private final boolean iconified;
    private final ProgressBar waitSpinner;

    private BigInteger tokenId;

    private StringBuilder attrs;

    private final AssetDefinitionService assetDefinitionService; //need to cache this locally, unless we cache every string we need in the constructor

    public AssetInstanceScriptHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService, boolean iconified)
    {
        super(resId, parent);
        iFrame = findViewById(R.id.iframe);
        iFrameLayout = findViewById(R.id.layout_select_ticket);
        tokenView = findViewById(R.id.token_frame);
        waitSpinner = findViewById(R.id.progress_element);
        iFrame.setVisibility(View.GONE);
        tokenView.setVisibility(View.VISIBLE);
        itemView.setOnClickListener(this);
        assetDefinitionService = assetService;
        token = t;
        tokenView.setChainId(token.tokenInfo.chainId);
        tokenView.setWalletAddress(new Address(token.getWallet()));
        tokenView.setRpcUrl(token.tokenInfo.chainId);
        tokenView.setOnReadyCallback(this);
        this.iconified = iconified;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        try
        {
            if (data.tokenIds.size() == 0) { fillEmpty(); return; }
            waitSpinner.setVisibility(View.VISIBLE);
            tokenView.setVisibility(View.GONE);
            getAttrs(data);
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    private void displayTicket(String tokenAttrs, TicketRange data)
    {
        waitSpinner.setVisibility(View.GONE);
        tokenView.setVisibility(View.VISIBLE);

        String viewType = iconified ? "view-iconified" : "view";
        String view = assetDefinitionService.getTokenView(token.tokenInfo.chainId, token.getAddress(), viewType);
        String style = assetDefinitionService.getTokenView(token.tokenInfo.chainId, token.getAddress(), "style");
        String viewData = tokenView.injectWeb3TokenInit(getContext(), view, tokenAttrs);
        viewData = tokenView.injectStyleData(viewData, style); //style injected last so it comes first

        tokenView.loadData(viewData, "text/html", "utf-8");

        if (iconified)
        {
            iFrameLayout.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), TokenFunctionActivity.class);
                intent.putExtra(TICKET, token);
                intent.putExtra(C.EXTRA_TOKEN_ID, token.intArrayToString(data.tokenIds, false));
                intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                getContext().startActivity(intent);
            });
        }
    }

    private void getAttrs(TicketRange data) throws Exception
    {
        tokenId = data.tokenIds.get(0);
        attrs = assetDefinitionService.getTokenAttrs(token, data.tokenIds.size());
        assetDefinitionService.resolveAttrs(token, tokenId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onAttr, this::onError, () -> displayTicket(attrs.toString(), data))
                .isDisposed();
    }

    private void onError(Throwable throwable)
    {
        throwable.printStackTrace();
        //fill attrs from database
        TokenScriptResult tsr = assetDefinitionService.getTokenScriptResult(token, tokenId);
        for (TokenScriptResult.Attribute attr : tsr.getAttributes().values())
        {
            try
            {
                TokenScriptResult.addPair(attrs, attr.id, attr.text);
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void onAttr(TokenScriptResult.Attribute attribute)
    {
        //add to string
        try
        {
            TokenScriptResult.addPair(attrs, attribute.id, attribute.text);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
    }

    private void fillEmpty()
    {
        iFrame.loadData("<html><body>No Data</body></html>", "text/html", "utf-8");
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPageLoaded()
    {
        tokenView.callToJS("refresh()");
    }
}
