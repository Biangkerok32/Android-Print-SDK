/*****************************************************
 *
 * AProductCreationFragment.java
 *
 *
 * Modified MIT License
 *
 * Copyright (c) 2010-2015 Kite Tech Ltd. https://www.kite.ly
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The software MAY ONLY be used with the Kite Tech Ltd platform and MAY NOT be modified
 * to be used with any competitor platforms. This means the software MAY NOT be modified 
 * to place orders with any competitors to Kite Tech Ltd, all orders MUST go through the
 * Kite Tech Ltd platform servers. 
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 *****************************************************/

///// Package Declaration /////

package ly.kite.journey.creation;


///// Import(s) /////

import android.content.Intent;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ly.kite.KiteSDK;
import ly.kite.R;
import ly.kite.image.IImageSizeConsumer;
import ly.kite.ordering.ImageSpec;
import ly.kite.util.Asset;
import ly.kite.journey.AImageSource;
import ly.kite.journey.AKiteFragment;
import ly.kite.journey.IImageSpecStore;
import ly.kite.catalogue.Product;
import ly.kite.image.ImageAgent;
import ly.kite.widget.PromptTextFrame;


///// Class Declaration /////

/*****************************************************
 *
 * This is the abstract super-class of product creation
 * fragments. It provides some common features.
 *
 *****************************************************/
abstract public class AProductCreationFragment extends    AKiteFragment
                                               implements View.OnClickListener,
                                                          PopupMenu.OnMenuItemClickListener,
                                                          AImageSource.IAssetConsumer

  {
  ////////// Static Constant(s) //////////

  @SuppressWarnings( "unused" )
  static public  final String  TAG               = "AProductCreationFrag.";

  static private final int     PROGRESS_COMPLETE = 100;  // 100%


  ////////// Static Variable(s) //////////


  ////////// Member Variable(s) //////////

  protected Product                       mProduct;
  protected ArrayList<ImageSpec>          mImageSpecArrayList;

  private   ProgressBar                   mProgressBar;
  private   PromptTextFrame               mPromptTextFrame;
  private   TextView                      mProceedOverlayTextView;
  private   TextView                      mCancelTextView;
  private   TextView                      mConfirmTextView;
  private   TextView                      mCTABarLeftTextView;
  private   TextView                      mCTABarRightTextView;
  private   View                          mAddImageView;

  protected boolean                       mShowPromptText;

  protected int                           mInitialImagesToCropCount;
  protected int                           mRemainingImagesToCropCount;


  ////////// Static Initialiser(s) //////////


  ////////// Static Method(s) //////////


  ////////// Constructor(s) //////////


  ////////// AKiteFragment Method(s) //////////

  /*****************************************************
   *
   * Called when the fragment is created.
   *
   *****************************************************/
  @Override
  public void onCreate( Bundle savedInstanceState )
    {
    super.onCreate( savedInstanceState );


    // Get the product

    Bundle arguments = getArguments();

    if ( arguments == null )
      {
      Log.e( TAG, "No arguments found" );

      return;
      }

    mProduct = arguments.getParcelable( BUNDLE_KEY_PRODUCT );


    if ( mProduct == null )
      {
      throw ( new IllegalStateException( "No product supplied" ) );
      }
    }


  /*****************************************************
   *
   * Called after the activity is created.
   *
   *****************************************************/
  @Override
  public void onActivityCreated( Bundle savedInstanceState )
    {
    super.onActivityCreated( savedInstanceState );


    // Only show the prompt animation the first time this
    // screen is displayed.

    if ( savedInstanceState == null && mPromptTextFrame != null )
      {
      mShowPromptText = true;
      }


    // We can't get the shared assets and quantity list until after the
    // activity has been created.

    if ( mKiteActivity != null && mKiteActivity instanceof IImageSpecStore )
      {
      mImageSpecArrayList = ( (IImageSpecStore)mKiteActivity ).getImageSpecArrayList();
      }

    if ( mImageSpecArrayList == null )
      {
      throw ( new IllegalStateException( "The image spec list could not be obtained" ) );
      }

    }


  /*****************************************************
   *
   * Called the first time the options menu is created.
   *
   *****************************************************/
  protected void onCreateOptionsMenu( Menu menu, MenuInflater menuInflator, int menuResourceId )
    {
    // The add photo XML has menu options for all the image sources, but they might
    // not all be enabled. So after we've inflated it, we need to go through an remove
    // any source that isn't available.

    menuInflator.inflate( menuResourceId, menu );

    MenuItem addImageItem = menu.findItem( R.id.add_image_menu_item );

    if ( addImageItem != null )
      {
      SubMenu addPhotoSubMenu = addImageItem.getSubMenu();

      if ( addPhotoSubMenu != null )
        {
        addImageSourceMenuItems( addPhotoSubMenu );
        }
      }

    }


  /*****************************************************
   *
   * Called when an options item is selected.
   *
   *****************************************************/
  @Override
  final public boolean onOptionsItemSelected( MenuItem item )
    {
    // Check for add image

    if ( onCheckAddImageOptionItem( item, getMaxAddImageCount() ) )
      {
      return ( true );
      }


    // Check for custom item

    if ( onCheckCustomOptionItem( item ) )
      {
      return ( true );
      }


    return ( super.onOptionsItemSelected( item ) );
    }



  /*****************************************************
   *
   * Called when an item in the options menu is selected.
   *
   *****************************************************/
  final protected boolean onCheckAddImageOptionItem( MenuItem item, int maxImageCount )
    {
    int itemId = item.getItemId();


    // If one of the image source menu items was selected - launch the appropriate picker

    AImageSource imageSource = KiteSDK.getInstance( mKiteActivity ).getImageSourceByMenuItemId( itemId );

    if ( imageSource != null )
      {
      imageSource.onPick( this, maxImageCount );

      return ( true );
      }


    return ( false );
    }


  /*****************************************************
   *
   * Called with the result of an activity.
   *
   *****************************************************/
  @Override
  public void onActivityResult( int requestCode, int resultCode, Intent returnedIntent )
    {
    super.onActivityResult( requestCode, resultCode, returnedIntent );

    // Get assets for any images returned and add them
    KiteSDK.getInstance( mKiteActivity ).getAssetsFromPickerResult( mKiteActivity, requestCode, resultCode, returnedIntent, this );
    }


  /*****************************************************
   *
   * Called when the fragment is top-most.
   *
   *****************************************************/
  @Override
  public void onTop()
    {
    super.onTop();

    // We don't enable the proceed button until all the assets have been cropped
    setForwardsTextViewEnabled( mRemainingImagesToCropCount < 1 );
    }


  ////////// View.OnClickListener Method(s) //////////

  /*****************************************************
   *
   * Called when something is clicked.
   *
   *****************************************************/
  @Override
  public void onClick( View view )
    {
    if ( mAddImageView != null && view == mAddImageView )
      {
      ///// Add photo /////

      displayAddImagePopupMenu( view );
      }
    }

  ////////// PopupMenu.OnMenuItemClickListener Method(s) //////////

  /*****************************************************
   *
   * Called when a pop-up menu item is clicked.
   *
   *****************************************************/
  @Override
  public boolean onMenuItemClick( MenuItem item )
    {
    if ( onCheckAddImageOptionItem( item, getMaxAddImageCount() ) )
      {
      return ( true );
      }

    return ( false );
    }


  ////////// AImageSource.IAssetConsumer Method(s) //////////

  /*****************************************************
   *
   * Called with new picked assets.
   *
   *****************************************************/
  @Override
  public void isacOnAssets( List<Asset> assetList )
    {
    if ( assetList != null )
      {
      onAddAssets( assetList );
      }
    }


  ////////// Method(s) //////////

  /*****************************************************
   *
   * Called by a child class when the view has been created.
   *
   *****************************************************/
  protected void onViewCreated( View view )
    {
    // Get references to any views
    mPromptTextFrame        = (PromptTextFrame)view.findViewById( R.id.prompt_text_frame );
    mProgressBar            = (ProgressBar)view.findViewById( R.id.progress_bar );
    mProceedOverlayTextView = (TextView)view.findViewById( R.id.proceed_overlay_text_view );
    mCancelTextView         = (TextView)view.findViewById( R.id.cancel_text_view );
    mConfirmTextView        = (TextView)view.findViewById( R.id.confirm_text_view );
    mCTABarLeftTextView     = (TextView)view.findViewById( R.id.cta_bar_left_text_view );
    mCTABarRightTextView    = (TextView)view.findViewById( R.id.cta_bar_right_text_view );
    mAddImageView           = view.findViewById( R.id.add_image_view );

    // If there is an add photo view - set the listener
    if ( mAddImageView != null ) mAddImageView.setOnClickListener( this );
    }


  /*****************************************************
   *
   * Adds menu items for all image sources.
   *
   *****************************************************/
  protected void addImageSourceMenuItems( Menu menu )
    {
    for ( AImageSource imageSource : KiteSDK.getInstance( mKiteActivity ).getAvailableImageSources() )
      {
      imageSource.addAsMenuItem( menu );
      }
    }


  /*****************************************************
   *
   * Displays a pop-up menu to add an image, using the
   * available image sources.
   *
   *****************************************************/
  protected PopupMenu displayAddImagePopupMenu( View view )
    {
    PopupMenu popupMenu = new PopupMenu( mKiteActivity, view );
    popupMenu.inflate( R.menu.add_image_popup );
    addImageSourceMenuItems( popupMenu.getMenu() );
    popupMenu.setOnMenuItemClickListener( this );
    popupMenu.show();

    return ( popupMenu );
    }


  /*****************************************************
   *
   * Returns a backwards text view.
   *
   *****************************************************/
  protected TextView getBackwardsTextView()
    {
    if ( mCTABarLeftTextView != null ) return ( mCTABarLeftTextView );
    if ( mCancelTextView     != null ) return ( mCancelTextView );

    return ( null );
    }


  /*****************************************************
   *
   * Sets the visibility of any backwards text view.
   *
   *****************************************************/
  protected void setBackwardsTextViewVisibility( int visibility )
    {
    TextView backwardsTextView = getBackwardsTextView();

    if ( backwardsTextView != null ) backwardsTextView.setVisibility( visibility );
    }


  /*****************************************************
   *
   * Sets the text of any backwards text view.
   *
   *****************************************************/
  protected void setBackwardsTextViewText( int textResourceId )
    {
    TextView backwardsTextView = getBackwardsTextView();

    if ( backwardsTextView != null ) backwardsTextView.setText( textResourceId );
    }


  /*****************************************************
   *
   * Sets the enabled state of any backwards text view.
   *
   *****************************************************/
  protected void setBackwardsTextViewEnabled( boolean enabled )
    {
    TextView backwardsTextView = getBackwardsTextView();

    if ( backwardsTextView != null ) backwardsTextView.setEnabled( enabled );
    }


  /*****************************************************
   *
   * Sets the listener for any backwards text view.
   *
   *****************************************************/
  protected void setBackwardsTextViewOnClickListener( View.OnClickListener listener )
    {
    TextView backwardsTextView = getBackwardsTextView();

    if ( backwardsTextView != null ) backwardsTextView.setOnClickListener( listener );
    }


  /*****************************************************
   *
   * Returns a forwards text view.
   *
   *****************************************************/
  protected TextView getForwardsTextView()
    {
    if ( mCTABarRightTextView    != null ) return ( mCTABarRightTextView );
    if ( mProceedOverlayTextView != null ) return ( mProceedOverlayTextView );
    if ( mConfirmTextView        != null ) return ( mConfirmTextView );

    return ( null );
    }


  /*****************************************************
   *
   * Sets the visibility of any forwards text view.
   *
   *****************************************************/
  protected void setForwardsTextViewVisibility( int visibility )
    {
    TextView forwardsTextView = getForwardsTextView();

    if ( forwardsTextView != null ) forwardsTextView.setVisibility( visibility );
    }


  /*****************************************************
   *
   * Sets the text of any forwards text view.
   *
   *****************************************************/
  protected void setForwardsTextViewText( int textResourceId )
    {
    TextView forwardsTextView = getForwardsTextView();

    if ( forwardsTextView != null ) forwardsTextView.setText( textResourceId );
    }


  /*****************************************************
   *
   * Sets the style of any forwards text view.
   *
   *****************************************************/
  protected void setForwardsTextViewBold( boolean bold )
    {
    TextView forwardsTextView = getForwardsTextView();

    if ( forwardsTextView != null )
      {
      forwardsTextView.setTypeface( forwardsTextView.getTypeface(), bold ? Typeface.BOLD : Typeface.NORMAL );
      }
    }


  /*****************************************************
   *
   * Sets the enabled state of any forwards text view.
   *
   *****************************************************/
  protected void setForwardsTextViewEnabled( boolean enabled )
    {
    TextView forwardsTextView = getForwardsTextView();

    if ( forwardsTextView != null ) forwardsTextView.setEnabled( enabled );
    }


  /*****************************************************
   *
   * Sets the listener for any forwards text view.
   *
   *****************************************************/
  protected void setForwardsTextViewOnClickListener( View.OnClickListener listener )
    {
    TextView forwardsTextView = getForwardsTextView();

    if ( forwardsTextView != null ) forwardsTextView.setOnClickListener( listener );
    }


  /*****************************************************
   *
   * Ensures that we have square cropped images for all
   * assets.
   *
   * @return true if there are images to crop, false
   *         otherwise.
   *
   *****************************************************/
  protected boolean requestCroppedAssets()
    {
    // First build a list of assets that need to be cropped, so we know how many there are
    // before we actually start requesting them. Note that the asset array list may be
    // sparsely populated for certain products (such as photobooks, which can have blank
    // pages).

    String productId = mProduct.getId();

    List<ImageSpec> imageSpecToCropList = new ArrayList<>( mImageSpecArrayList.size() );

    for ( ImageSpec imageSpec : mImageSpecArrayList )
      {
      if ( imageSpec != null )
        {
        // If we don't already have a crop rectangle - create one now

        if ( ( productId == null ) || ( ! productId.equals( imageSpec.getCroppedForProductId() ) ) )
          {
          imageSpecToCropList.add( imageSpec );
          }
        }
      }


    // Get the counts

    mRemainingImagesToCropCount = mInitialImagesToCropCount = imageSpecToCropList.size();

    showProgress( mRemainingImagesToCropCount, mInitialImagesToCropCount );


    // Now go back through and request all the images

    for ( ImageSpec imageSpec : imageSpecToCropList )
      {
      requestCroppedImage( imageSpec );
      }


    boolean assetsToCrop = mInitialImagesToCropCount > 0;

    // Set the enabled state of the proceed button according to whether there are assets to crop
    setForwardsTextViewEnabled( assetsToCrop );

    return ( assetsToCrop );
    }


  /*****************************************************
   *
   * Requests a single square cropped image for an asset.
   *
   *****************************************************/
  protected void requestCroppedImage( ImageSpec imageSpec )
    {
    ImageAgent.with( mKiteActivity )
            .loadSizeOf( imageSpec.getAssetFragment().getAsset() )
            .into( new ImageCropCallback( imageSpec ) );
    }


  /*****************************************************
   *
   * Shows the cropping progress.
   *
   *****************************************************/
  protected void showProgress( int remainingCount, int totalCount )
    {
    if ( mProgressBar == null ) return;


    // If there are no images, or none left to crop, don't show the
    // progress bar.

    if ( totalCount < 1 || remainingCount < 1 )
      {
      mProgressBar.setVisibility( View.INVISIBLE );
      }
    else
      {
      mProgressBar.setVisibility( View.VISIBLE );

      mProgressBar.setProgress( PROGRESS_COMPLETE * ( totalCount - remainingCount ) / totalCount );
      }
    }


  /*****************************************************
   *
   * Called when an image is cropped.
   *
   *****************************************************/
  protected void onImageCropped( ImageSpec imageSpec )
    {
    }


  /*****************************************************
   *
   * Called when all images have been cropped.
   *
   *****************************************************/
  protected void onAllImagesCropped()
    {
    }


  /*****************************************************
   *
   * Called by a child class when everything has been loaded
   * or cropped, and the screen is ready to be used.
   *
   *****************************************************/
  protected void onScreenReady()
    {
    // Display any prompt text
    if ( mShowPromptText && mPromptTextFrame != null )
      {
      mShowPromptText = false;

      mPromptTextFrame.startDisplayCycle();
      }
    }


  /*****************************************************
   *
   * Called when an item in the options menu is selected,
   * which has not already been handled.
   *
   *****************************************************/
  protected boolean onCheckCustomOptionItem( MenuItem item )
    {
    return ( false );
    }


  /*****************************************************
   *
   * Called when a new asset is added. Will be called multiple
   * times if a number of assets are added together.
   *
   *****************************************************/
  protected void onAssetAdded( ImageSpec imageSpec )
    {
    }


  /*****************************************************
   *
   * Returns the maximum number of images required when
   * adding images.
   *
   *****************************************************/
  protected int getMaxAddImageCount()
    {
    return ( AImageSource.UNLIMITED_IMAGES );
    }


  /*****************************************************
   *
   * Adds new unedited assets to the end of the current list.
   * Duplicates will be discarded.
   *
   *****************************************************/
  protected void onAddAssets( List<Asset> newAssetList )
    {
    for ( Asset asset : newAssetList )
      {
      // We don't allow duplicate images, so first check that the asset isn't already in
      // our list. Note that we don't check the scenario where the image is the same but
      // from a different source - a byte by byte comparison would take too long, and a
      // duplicate is unlikely anyway.

      if ( ! ImageSpec.assetIsInList( mImageSpecArrayList, asset ) )
        {
        // Start with the original asset, and a quantity of 1.
        ImageSpec imageSpec = new ImageSpec( asset );

        // Add the selected image to our asset lists, mark it as checked
        mImageSpecArrayList.add( imageSpec );

        onAssetAdded( imageSpec );
        }
      }


    onNewAssetsPossiblyAdded();
    }


  /*****************************************************
   *
   * Inserts new unedited assets into the current list,
   * filling in any empty slots from the supplied position.
   *
   *****************************************************/
  protected void onAddAssets( List<Asset> newAssetList, int firstInsertionPointIndex, boolean appendIfFull )
    {
    int imageSpecCount = mImageSpecArrayList.size();

    int insertionPointIndex = nextInsertionPointIndexFrom( firstInsertionPointIndex );

    for ( Asset asset : newAssetList )
      {
      ImageSpec imageSpec = new ImageSpec( asset );

      if ( insertionPointIndex < imageSpecCount )
        {
        mImageSpecArrayList.set( insertionPointIndex, imageSpec );
        }
      else
        {
        if ( appendIfFull ) mImageSpecArrayList.add( imageSpec );
        else                break;
        }

      onAssetAdded( imageSpec );

      insertionPointIndex = nextInsertionPointIndexFrom( insertionPointIndex );
      }


    onNewAssetsPossiblyAdded();
    }

  /*****************************************************
   *
   * Finds the next insertion point from that supplied.
   *
   *****************************************************/
  int nextInsertionPointIndexFrom( int startIndex )
    {
    int imageSpecCount = mImageSpecArrayList.size();

    int index = startIndex;

    for ( ; index < imageSpecCount && mImageSpecArrayList.get( index ) != null; index ++ );

    return ( index );
    }


  /*****************************************************
   *
   * Called after new assets have been added. Note that the
   * number of assets added may be 0.
   *
   *****************************************************/
  protected void onNewAssetsPossiblyAdded()
    {
    // Get cropped versions of any new assets, and call back to the child class if
    // there were some new ones.
    if ( requestCroppedAssets() )
      {
      onNewAssetsBeingCropped();

      setForwardsTextViewEnabled( false );
      }
    }


  /*****************************************************
   *
   * Called when at least one new asset has been added.
   *
   *****************************************************/
  protected void onNewAssetsBeingCropped()
    {
    }


  /*****************************************************
   *
   * Displays a dialog informaing the user that they have
   * not filled a pack.
   *
   *****************************************************/
  protected void displayNotFullDialog( int expectedImageCount, int actualImageCount, Runnable proceedRunnable )
    {
    mKiteActivity.displayModalDialog(
      getString( R.string.alert_dialog_title_pack_not_full_format_string, getResources().getQuantityString( R.plurals.photo_plurals, actualImageCount, actualImageCount ) ),
      getString( R.string.alert_dialog_message_pack_not_full_format_string, expectedImageCount - actualImageCount ),
      R.string.print_these,
      proceedRunnable,
      R.string.add_more, null );
    }


  ////////// Inner Class(es) //////////

  /*****************************************************
   *
   * The callback for the image cropping.
   *
   *****************************************************/
  private class ImageCropCallback implements IImageSizeConsumer
    {
    private ImageSpec  mImageSpec;


    ImageCropCallback( ImageSpec imageSpec )
      {
      mImageSpec = imageSpec;
      }


    ////////// IImageSizeConsumer Method(s) //////////

    /*****************************************************
     *
     * Called on the UI thread, with the image size.
     *
     *****************************************************/
    @Override
    public void onImageSizeAvailable( int width, int height )
      {
      // Calculate and save the crop rectangle

      RectF cropRectangle = ImageAgent.getProportionalCropRectangle( width, height, mProduct.getImageAspectRatio() );

      mImageSpec.setProportionalCropRectangle( cropRectangle, mProduct.getId() );


      onImageCropped( mImageSpec );


      // If we now have all the cropped images - enable the proceed button.

      mRemainingImagesToCropCount--;

      showProgress( mRemainingImagesToCropCount, mInitialImagesToCropCount );

      if ( mRemainingImagesToCropCount < 1 )
        {
        setForwardsTextViewEnabled( true );

        onAllImagesCropped();
        }
      }


    /*****************************************************
     *
     * Called when an image could not be loaded.
     *
     *****************************************************/
    @Override
    public void onImageSizeUnavailable( Exception exception )
      {
      // TODO
      }

    }

  }

