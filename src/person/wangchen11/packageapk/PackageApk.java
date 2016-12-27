package person.wangchen11.packageapk;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import person.wangchen11.console.Console;
import person.wangchen11.console.Terminal;
import person.wangchen11.cproject.CProject;
import person.wangchen11.filebrowser.Open;
import person.wangchen11.gnuccompiler.GNUCCompiler;
import person.wangchen11.gnuccompiler.GNUCCompiler2;
import person.wangchen11.xqceditor.R;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class PackageApk implements OnClickListener{
	final static String TAG="PackageApk";
	private static final int BUFFER = 4096;
	private Context mContext;
	private CProject mProject;
	private AlertDialog mAlertDialog;
	private Terminal mTerminal;
	private TextView mConsoleView;
	private ScrollView mScrollView;
	private boolean mIsAlive=true;
	private static ExecutorService mExecutorService;
	private Handler mHandler=null;
	@SuppressLint("InflateParams")
	public PackageApk(@NonNull Context context,@NonNull CProject cProject) {
		mHandler=new Handler();
		mContext=context;
		mProject=cProject;
		LayoutInflater inflater=LayoutInflater.from(context);
		ViewGroup viewGroup=(ViewGroup) inflater.inflate(R.layout.dialog_package_apk, null);
		mScrollView=(ScrollView) viewGroup.findViewById(R.id.scroll_view_package_apk);
		mConsoleView = (TextView) viewGroup.findViewById(R.id.text_view_console);
		AlertDialog.Builder builder=new Builder(context);
		builder.setTitle(R.string.package_and_run);
		builder.setView(viewGroup);
		builder.setCancelable(false);
		builder.setNegativeButton(R.string.cancel, this);
		mAlertDialog=builder.create();
		
	}

	private void addTextLn(String str)
	{
		addText(str+"\n");
	}
	
	private void addText(final String str)
	{
		mScrollView.post(new Runnable() {
			@Override
			public void run() {
				mConsoleView.setText( mConsoleView.getText().toString()+str);
				mScrollView.post(new Runnable() {
					@Override
					public void run() {
						mScrollView.fullScroll(View.FOCUS_DOWN);
					}
				});
			}
		});
	}
	
	public void start()
	{
		mIsAlive=true;
		mAlertDialog.show();
		Aapt.freeResourceIfNeed(mContext);
		freeResourceIfNeed();
		mTerminal=new Terminal(mHandler, new Console.ConsoleCallback() {
			
			@Override
			public void onReadData(Console console, byte[] data, int len,
					boolean isError) {
				if(mIsAlive)
				{
					addText( new String(data,0,len) );
				}
			}
			@Override
			public void onConsoleClosed(Console console) {
				if(mExecutorService==null)
				{
					mExecutorService=Executors.newSingleThreadExecutor();
				}
				mExecutorService.execute(new Runnable() {
					@Override
					public void run() {
						onCompileComplete();
					}
				});
			}
		},mContext);
		String cmd="";
		List<File > files=mProject.getAllCFiles();
		if(files.size()>0)
		{
			if(mProject.isGuiProject())
				cmd=GNUCCompiler2.getCompilerCmd(mContext, mProject, true);
				//cmd=GNUCCompiler.getProjectCompilerSoCmd(mContext, files, new File(mProject.getSoFilePath()), mProject.getOtherOption() );
			else
				cmd=GNUCCompiler2.getCompilerCmd(mContext, mProject, false);
				//cmd=GNUCCompiler.getProjectCompilerCmd(mContext, files, new File(mProject.getBinFilePath()), mProject.getOtherOption() );
		}
		else
			cmd="echo '"+
					mContext.getText(R.string.c_file_not_found)+
					"'\n";
		mTerminal.execute(cmd+"\nexit\n");
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_NEGATIVE:
			mIsAlive=false;
			mAlertDialog.cancel();
			if(mTerminal!=null)
				mTerminal.destory();
			break;

		default:
			break;
		}
	}

	public String getTempPath()
	{
		return mProject.getBinPath()+"/temp";
	}
	
	public String getDexPathTo()
	{
		return getTempPath()+"/classes.dex";
	}

	public String getDexPathFrom()
	{
		return mProject.getProjectPath()+"/classes.dex";
	}
	
	public String getUnsignedApkPath()
	{
		return mProject.getBinPath()+File.separator+mProject.getProjectName()+".zip";
	}
	

	public String getSignedApkPath()
	{
		return mProject.getBinPath()+File.separator+mProject.getProjectName()+".apk";
	}

	public String getPemPath()
	{
		return mContext.getFilesDir()+"/security/testkey.x509.pem";
	}
	
	public String getPk8Path()
	{
		return mContext.getFilesDir()+"/security/testkey.pk8";
	}

	@SuppressWarnings("resource")
	public static boolean freeZip(String fileIn,String pathTo){
		if(pathTo!=null)
			pathTo+=File.separatorChar;
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(new File(fileIn)));
			BufferedOutputStream dest = null;
			ZipEntry entry = null; 
			String strEntry = null;
			byte data[] = new byte[BUFFER];
			while ((entry = zis.getNextEntry()) != null) {
				try {
					Log.i("Unzip: ", "" + entry);
					int count;
					strEntry = entry.getName();

					File entryFile = new File(pathTo + strEntry);
					File entryDir = new File(entryFile.getParent());
					entryDir.mkdirs();
					if (!entryDir.exists()) {
						Log.i(TAG, "mkdirs");
						if(!entryDir.mkdirs())
							Log.i(TAG, "mkdirs failed :"+entryDir.getAbsolutePath());;
					}
					if(entry.isDirectory())
					{
						entryFile.mkdirs();
					}else
					{
						FileOutputStream fos = new FileOutputStream(entryFile);
						dest = new BufferedOutputStream(fos, BUFFER);
						while ((count = zis.read(data, 0, BUFFER)) != -1) {
							dest.write(data, 0, count);
						}
						dest.flush();
						dest.close();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					return false;
				}
			}
			zis.close();
		} catch (Exception cwj) {
			cwj.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void onCompileComplete()
	{
		if(mIsAlive)
		{
			if( 	(mProject.isGuiProject()&&new File(mProject.getSoFilePath()).isFile())
				||  (!mProject.isGuiProject()&&new File(mProject.getBinFilePath()).isFile()) )
			{
				mTerminal.destory();
				mTerminal=new Terminal(mHandler, new Console.ConsoleCallback() {
					
					@Override
					public void onReadData(Console console, byte[] data, int len,
							boolean isError) {
						if(mIsAlive)
						{
							addText( new String(data,0,len) );
						}
					}
					
					@Override
					public void onConsoleClosed(Console console) {
						onAaptComplete();
					}
				},mContext);
				String cmd=Aapt.getPackageApkCmd(mContext, mProject);
				new File(mProject.getResZipPath()).delete();
				mTerminal.execute(cmd+"\nexit\n");
				
			}
			else
			{
				addTextLn("");
				addTextLn( mContext.getString(R.string.compile_has_error) );
			}
		}
	}
	
	private void onAaptComplete()
	{
		if(mIsAlive)
		{
			if(new File(mProject.getResZipPath()).isFile())
			{
				if( new File(mProject.getResZipPath()).isFile() )
				{
					SignApk();
				}
				else
				{
					addTextLn(mContext.getString(R.string.package_fail));
				}
			}
			else
			{
				addTextLn(mContext.getString(R.string.deal_res_fail));
			}
		}
	}
	
	private void SignApk()
	{
		String []args={
				getPemPath(),//"platform.x509.pem",
				getPk8Path(),//"platform.pk8",
				mProject.getResZipPath(),
				getSignedApkPath()
		};
		new File(getSignedApkPath()).delete();
		person.wangchen11.signapk.SignApk.mymain(args);
		if(new File(getSignedApkPath()).isFile())
		{
			addTextLn(mContext.getString(R.string.signed_apk_done));
			Open.openFile(mContext, new File(getSignedApkPath()));
			mIsAlive=false;
			//mAlertDialog.cancel();
			mAlertDialog.setCancelable(true);
		}
		else
		{
			addTextLn(mContext.getString(R.string.signed_apk_fail));
		}
	}

	public boolean freeResourceIfNeed()
	{
		if(!new File(getPemPath()).isFile())
		{
			return GNUCCompiler.freeZip(mContext, "security.zip", mContext.getFilesDir().getPath());
		}
		return true;
	}
}
