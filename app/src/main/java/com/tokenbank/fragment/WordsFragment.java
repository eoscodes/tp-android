package com.tokenbank.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tokenbank.R;
import com.tokenbank.activity.MainActivity;
import com.tokenbank.activity.WebBrowserActivity;
import com.tokenbank.base.BlockChainData;
import com.tokenbank.base.BaseWalletUtil;
import com.tokenbank.base.WalletInfoManager;
import com.tokenbank.base.WCallback;
import com.tokenbank.base.TBController;
import com.tokenbank.config.Constant;
import com.tokenbank.utils.FileUtil;
import com.tokenbank.utils.GsonUtil;
import com.tokenbank.utils.ToastUtil;
import com.tokenbank.utils.ViewUtil;

import java.util.List;


public class WordsFragment extends BaseFragment implements View.OnClickListener {
    public final static String TAG = "PKFragment";
    private EditText mEdtWalletWords;
    private RelativeLayout mLayoutSelectBlockChain;
    private TextView mTvBlockChain;
    private EditText mEdtWalletName;
    private EditText mEdtWalletPwd;
    private EditText mEdtWalletPwdRepeat;
    private EditText mEdtWalletPwdTips;
    private ImageView mImgboxTerms;
    private TextView mTvTerms;
    private TextView mTvImportWallet;
    private BaseWalletUtil mWalletUtil;
    private int flag = 1;
    private final static String FLAG = "Flag";
    private final static String BLOCK = "Block";
    private BlockChainData.Block mBlock;

    public static WordsFragment newInstance(int flag, BlockChainData.Block block) {

        Bundle args = new Bundle();

        WordsFragment fragment = new WordsFragment();
        args.putInt(FLAG, flag);
        args.putParcelable(BLOCK, block);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            flag = getArguments().getInt(FLAG, 1);
            mBlock = getArguments().getParcelable(BLOCK);
        }
        if (mBlock == null) {
            getActivity().finish();
            return;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return ViewUtil.inflatView(inflater, container, R.layout.fragment_words_importwallet, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        initView(view);
    }

    @Override
    public void onClick(View view) {
        if (view == mTvTerms) {
            gotoServiceTermPage();
        } else if (view == mTvImportWallet) {
            if (paramCheck()) {
                importWallet();
            }
        } else if (view == mImgboxTerms) {
            mImgboxTerms.setSelected(!mImgboxTerms.isSelected());
        }
    }


    private void initView(View view) {

        mEdtWalletWords = view.findViewById(R.id.edt_wallet_words);

        mLayoutSelectBlockChain = view.findViewById(R.id.layout_block_chain);
        mLayoutSelectBlockChain.setOnClickListener(this);
        mTvBlockChain = view.findViewById(R.id.tv_block_chain);

        mEdtWalletName = view.findViewById(R.id.edt_wallet_name);

        mEdtWalletPwd = view.findViewById(R.id.edt_wallet_pwd);
        mEdtWalletPwdRepeat = view.findViewById(R.id.edt_wallet_pwd_repeat);
        mEdtWalletPwdTips = view.findViewById(R.id.edt_pwd_tips);


        mImgboxTerms = view.findViewById(R.id.img_service_terms);
        mImgboxTerms.setOnClickListener(this);

        mTvTerms = view.findViewById(R.id.tv_service_terms);
        mTvTerms.setText(Html.fromHtml("<font color='#565d67'>我已经仔细阅读并同意</font><font color='#2890FE'>服务及隐私条款</font>"));
        mTvTerms.setOnClickListener(this);

        mTvImportWallet = view.findViewById(R.id.tv_import_wallet);
        mTvImportWallet.setOnClickListener(this);

        mTvBlockChain.setText(mBlock.desc);
        mWalletUtil = TBController.getInstance().getWalletUtil((int) mBlock.hid);

    }

    private void gotoServiceTermPage() {
        WebBrowserActivity.startWebBrowserActivity(getActivity(), "用户服务协议", Constant.service_term_url);
    }

    private boolean paramCheck() {
        String walletWords = mEdtWalletWords.getText().toString();
        String walletPwd = mEdtWalletPwd.getText().toString();
        String walletPwdRepeat = mEdtWalletPwdRepeat.getText().toString();
        String walletName = mEdtWalletName.getText().toString();
        boolean readedTerms = mImgboxTerms.isSelected();
        if (TextUtils.isEmpty(walletName)) {
            ViewUtil.showSysAlertDialog(getActivity(), "请输入钱包名字", "OK");
            return false;
        }
        if (TextUtils.isEmpty(walletWords) || walletWords.split(" ") == null || walletWords.split(" ").length < 12) {
            ViewUtil.showSysAlertDialog(getActivity(), "助记词不正确", "OK");
            return false;
        }
        if (TextUtils.isEmpty(walletPwd)) {
            ViewUtil.showSysAlertDialog(getActivity(), "密码不能为空", "OK");
            return false;
        }

        if (TextUtils.isEmpty(walletPwdRepeat)) {
            ViewUtil.showSysAlertDialog(getActivity(), "密码重复不能为空", "OK");
            return false;
        }

        if (!TextUtils.equals(walletPwdRepeat, walletPwd)) {
            ViewUtil.showSysAlertDialog(getActivity(), "两次密码不同，请重新输入", "OK");
            return false;
        }
        if (walletPwd.length() < 8) {
            ViewUtil.showSysAlertDialog(getActivity(), "密码长度不能小于8位", "OK");
            return false;
        }
        if (!readedTerms) {
            ViewUtil.showSysAlertDialog(getActivity(), "您尚未阅读并同意服务和隐私条款", "OK");
            return false;
        }
        return true;
    }

    private void importWallet() {
        final String words = mEdtWalletWords.getText().toString();
        final String password = mEdtWalletPwd.getText().toString();
        mWalletUtil.importWallet(words, (int) mBlock.hid, 1, new WCallback() {
            @Override
            public void onGetWResult(int ret, GsonUtil extra) {
                if (ret == 0) {
                    String address = extra.getString("address", "");
                    String privateKey = extra.getString("privateKey", "");
                    if (isWalletExsit(address)) {
                        if (flag == 1) {
                            //导入钱包
                            ToastUtil.toast(getActivity(), "钱包已经存在,不需要导入");
                            return;
                        } else if (flag == 2) {
                            //重置密码
                            WalletInfoManager.getInstance().updateWalletHash(address, FileUtil.getStringContent(password));
                            return;
                        }

                    }
                    uploadWallet(mEdtWalletName.getText().toString(), extra.getInt("blockType", -1), FileUtil.getStringContent(password),
                            privateKey, address);
                } else {
                    ToastUtil.toast(getActivity(), "导入钱包失败");
                }
            }
        });
    }

    private void uploadWallet(final String name, final int walletType, final String hash, final String privateKey,
                              final String address) {
        long walletId = System.currentTimeMillis();
        storeWallet(walletId, walletType, name, address, hash, privateKey);
        gotoMainActivity();
    }

    private void gotoMainActivity() {
        // 添加资产时，进入创建钱包
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

    private void storeWallet(long walletId, int walletType, String walletName, String address, String walletHash, String privatekey) {
        WalletInfoManager.WData wallet = new WalletInfoManager.WData();
        wallet.wid = walletId;
        wallet.wname = walletName;
        wallet.waddress = address;
        wallet.whash = walletHash;
        wallet.wpk = privatekey;
        wallet.type = walletType;
        wallet.words = "";
        wallet.isBaked = true;
        WalletInfoManager.getInstance().insertWallet(wallet);
    }

    private boolean isWalletExsit(String address) {
        List<WalletInfoManager.WData> allWallet = WalletInfoManager.getInstance().getAllWallet();
        if (allWallet == null || allWallet.size() <= 0) {
            return false;
        }
        for (WalletInfoManager.WData walletData : allWallet) {
            if (TextUtils.equals(walletData.waddress, address)) {
                return true;
            }
        }
        return false;
    }
}
