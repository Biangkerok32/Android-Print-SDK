/*****************************************************
 *
 * PaymentActivity.java
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

package ly.kite.checkout;


///// Import(s) /////

import java.math.BigDecimal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.paypal.android.sdk.payments.ProofOfPayment;

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;

import ly.kite.analytics.Analytics;
import ly.kite.api.OrderState;
import ly.kite.pricing.IPricingConsumer;
import ly.kite.pricing.OrderPricing;
import ly.kite.pricing.PricingAgent;
import ly.kite.KiteSDK;
import ly.kite.catalogue.MultipleCurrencyAmount;
import ly.kite.catalogue.PrintOrder;
import ly.kite.R;
import ly.kite.paypal.PayPalCard;
import ly.kite.paypal.PayPalCardChargeListener;
import ly.kite.paypal.PayPalCardVaultStorageListener;
import ly.kite.journey.AKiteActivity;
import ly.kite.catalogue.SingleCurrencyAmount;


///// Class Declaration /////

/*****************************************************
 *
 * This activity displays the price / payment screen.
 *
 *****************************************************/
public class PaymentActivity extends AKiteActivity implements IPricingConsumer,
                                                              TextView.OnEditorActionListener
  {
  ////////// Static Constant(s) //////////

  @SuppressWarnings("unused")
  private static final String LOG_TAG = "PaymentActivity";

  public static final String KEY_ORDER = "ly.kite.ORDER";

  public static final String ENVIRONMENT_STAGING = "ly.kite.ENVIRONMENT_STAGING";
  public static final String ENVIRONMENT_LIVE = "ly.kite.ENVIRONMENT_LIVE";
  public static final String ENVIRONMENT_TEST = "ly.kite.ENVIRONMENT_TEST";

  private static final String CARD_IO_TOKEN = "f1d07b66ad21407daf153c0ac66c09d7";

  private static final int REQUEST_CODE_PAYPAL = 0;
  private static final int REQUEST_CODE_CREDITCARD = 1;
  private static final int REQUEST_CODE_RECEIPT = 2;


  ////////// Static Variable(s) //////////


  ////////// Member Variable(s) //////////

  private PrintOrder           mOrder;
  private String               mAPIKey;
  private KiteSDK.Environment  mKiteSDKEnvironment;
  //private PayPalCard.Environment mPayPalEnvironment;

  private ListView mOrderSummaryListView;
  private EditText mPromoEditText;
  private Button mPromoButton;
  private Button mCreditCardButton;
  private Button mPayPalButton;
  private ProgressBar mProgressBar;

  private OrderPricing mOrderPricing;

  private boolean mPromoActionClearsCode;
  private String mLastSubmittedPromoCode;
  private boolean mLastPriceRetrievalSucceeded;


  ////////// Static Initialiser(s) //////////


  ////////// Static Method(s) //////////

  public static void startForResult( Activity activity, PrintOrder printOrder, int requestCode )
    {
    Intent intent = new Intent( activity, PaymentActivity.class );

    intent.putExtra( KEY_ORDER, printOrder );

    activity.startActivityForResult( intent, requestCode );
    }


  ////////// Constructor(s) //////////


  ////////// Activity Method(s) //////////

  /*****************************************************
   *
   * Called when the activity is created.
   *
   *****************************************************/
  @Override
  public void onCreate( Bundle savedInstanceState )
    {
    super.onCreate( savedInstanceState );


    // First look for a saved order (because it might have changed since we were first
    // created. If none if found - get it from the intent.

    if ( savedInstanceState != null )
      {
      mOrder = savedInstanceState.getParcelable( KEY_ORDER );
      }

    if ( mOrder == null )
      {
      Intent intent = getIntent();

      if ( intent != null )
        {
        mOrder = intent.getParcelableExtra( KEY_ORDER );
        }
      }

    if ( mOrder == null )
      {
      throw new IllegalArgumentException( "There must either be a saved Print Order, or one supplied in the intent used to start the Payment Activity" );
      }


    mKiteSDKEnvironment = KiteSDK.getInstance( this ).getEnvironment();


        /*
         * Start PayPal Service
         */

    PayPalConfiguration payPalConfiguration = new PayPalConfiguration()
            .clientId( mKiteSDKEnvironment.getPayPalClientId() )
            .environment( mKiteSDKEnvironment.getPayPalEnvironment() )
            .acceptCreditCards( false );

    Intent intent = new Intent( this, PayPalService.class );
    intent.putExtra( PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfiguration );

    startService( intent );


    // Set up the screen

    setContentView( R.layout.screen_payment );

    mOrderSummaryListView = (ListView) findViewById( R.id.order_summary_list_view );
    mPromoEditText        = (EditText) findViewById( R.id.promo_edit_text );
    mPromoButton          = (Button) findViewById( R.id.promo_button );
    mCreditCardButton     = (Button) findViewById( R.id.credit_card_button );
    mPayPalButton         = (Button) findViewById( R.id.paypal_button );
    mProgressBar          = (ProgressBar) findViewById( R.id.progress_bar );

    mPromoEditText.addTextChangedListener( new PromoCodeTextWatcher() );
    mPromoEditText.setOnEditorActionListener( this );

    hideKeyboard();

    if ( mKiteSDKEnvironment.getPayPalEnvironment().equals( PayPalConfiguration.ENVIRONMENT_SANDBOX ) )
      {
      setTitle( "Payment (Sandbox)" );
      }
    else
      {
      setTitle( "Payment" );
      }


    // Get the pricing information
    requestPrices();


    if ( savedInstanceState == null )
      {
      Analytics.getInstance( this ).trackPaymentScreenViewed( mOrder );
      }
    }


  @Override
  public void onSaveInstanceState( Bundle outState )
    {
    super.onSaveInstanceState( outState );

    outState.putParcelable( KEY_ORDER, mOrder );
    }


  @Override
  protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
    if ( requestCode == REQUEST_CODE_PAYPAL )
      {
      if ( resultCode == Activity.RESULT_OK )
        {

        PaymentConfirmation paymentConfirmation = data.getParcelableExtra( com.paypal.android.sdk.payments.PaymentActivity.EXTRA_RESULT_CONFIRMATION );

        if ( paymentConfirmation != null )
          {

          try
            {

            ProofOfPayment proofOfPayment = paymentConfirmation.getProofOfPayment();

            if ( proofOfPayment != null )
              {
              String paymentId = proofOfPayment.getPaymentId();

              if ( paymentId != null )
                {
                submitOrderForPrinting( paymentId );
                }
              else
                {
                showErrorDialog( "No payment id found in proof of payment" );
                }
              }
            else
              {
              showErrorDialog( "No proof of payment found in payment confirmation" );
              }

            }
          catch ( Exception exception )
            {
            showErrorDialog( exception.getMessage() );
            }
          }
        else
          {
          showErrorDialog( "No payment confirmation received from PayPal" );
          }
        }
      }
    else if ( requestCode == REQUEST_CODE_CREDITCARD )
      {
      if ( data != null && data.hasExtra( CardIOActivity.EXTRA_SCAN_RESULT ) )
        {
        CreditCard scanResult = data.getParcelableExtra( CardIOActivity.EXTRA_SCAN_RESULT );

        if ( !scanResult.isExpiryValid() )
          {
          showErrorDialog( "Sorry it looks like that card has expired. Please try again." );

          return;
          }

        PayPalCard card = new PayPalCard();
        card.setNumber( scanResult.cardNumber );
        card.setExpireMonth( scanResult.expiryMonth );
        card.setExpireYear( scanResult.expiryYear );
        card.setCvv2( scanResult.cvv );
        card.setCardType( PayPalCard.CardType.getCardType( scanResult.getCardType() ) );

        if ( card.getCardType() == PayPalCard.CardType.UNSUPPORTED )
          {
          showErrorDialog( "Sorry we couldn't recognize your card. Please try again manually entering your card details if necessary." );

          return;
          }

        final ProgressDialog dialog = new ProgressDialog( this );
        dialog.setCancelable( false );
        dialog.setTitle( "Processing" );
        dialog.setMessage( "One moment" );
        dialog.show();
        card.storeCard( mKiteSDKEnvironment, new PayPalCardVaultStorageListener()
        {
        @Override
        public void onStoreSuccess( PayPalCard card )
          {
          if ( dialog.isShowing() ) dialog.dismiss();

          payWithExistingCard( card );
          }

        @Override
        public void onError( PayPalCard card, Exception ex )
          {
          if ( dialog.isShowing() ) dialog.dismiss();

          showErrorDialog( ex.getMessage() );
          }
        } );

        }
      else
        {
        // card scan cancelled
        }
      }
    else if ( requestCode == REQUEST_CODE_RECEIPT )
      {
      setResult( Activity.RESULT_OK );
      finish();
      }
    }

  @Override
  public void onDestroy()
    {
    stopService( new Intent( this, PayPalService.class ) );
    super.onDestroy();
    }


  @Override
  public boolean onMenuItemSelected( int featureId, MenuItem item )
    {
    if ( item.getItemId() == android.R.id.home )
      {
      finish();
      return true;
      }
    return super.onMenuItemSelected( featureId, item );
    }


  ////////// IPricingConsumer Method(s) //////////

  /*****************************************************
   *
   * Called when the prices are successfully retrieved.
   *
   *****************************************************/
  @Override
  public void paOnSuccess( OrderPricing pricing )
    {
    mOrderPricing                = pricing;

    mLastPriceRetrievalSucceeded = true;

    mPromoButton.setEnabled( true );
    mCreditCardButton.setEnabled( true );
    mPayPalButton.setEnabled( true );

    mProgressBar.setVisibility( View.GONE );


    onGotPrices();
    }


  /*****************************************************
   *
   * Called when the prices could not be retrieved.
   *
   *****************************************************/
  @Override
  public void paOnError( Exception exception )
    {
    mLastPriceRetrievalSucceeded = false;

    displayModalDialog
      (
      R.string.alert_dialog_title_oops,
      getString( R.string.alert_dialog_message_pricing_format_string, exception.getMessage() ),
      R.string.Retry,
      new RetrievePricingRunnable(),
      R.string.Cancel,
      new FinishRunnable()
      );
    }


  ////////// TextView.OnEditorActionListener Method(s) //////////

  /*****************************************************
   *
   * Called when an action occurs on the editor. We use this
   * to determine when the done button is pressed on the on-screen
   * keyboard.
   *
   *****************************************************/
  @Override
  public boolean onEditorAction( TextView v, int actionId, KeyEvent event )
    {
    if ( actionId == EditorInfo.IME_ACTION_DONE )
      {
      onPerformPromoAction();
      }

    // Return false even if we intercepted the done - so the keyboard
    // will be hidden.

    return ( false );
    }


  ////////// Method(s) //////////

  /*****************************************************
   *
   * Requests pricing information.
   *
   *****************************************************/
  void requestPrices()
    {
    mLastSubmittedPromoCode = mPromoEditText.getText().toString();

    if ( mLastSubmittedPromoCode.trim().equals( "" ) ) mLastSubmittedPromoCode = null;

    mOrderPricing  = PricingAgent.getInstance().requestPricing( this, mOrder, mLastSubmittedPromoCode, this );


    // If the pricing wasn't cached - disable the buttons, and show the progress spinner, whilst
    // they are retrieved.

    if ( mOrderPricing == null )
      {
      mPromoButton.setEnabled( false );
      mCreditCardButton.setEnabled( false );
      mPayPalButton.setEnabled( false );

      mProgressBar.setVisibility( View.VISIBLE );

      return;
      }


    onGotPrices();
    }


  /*****************************************************
   *
   * Updates the screen once we have retrieved the pricing
   * information.
   *
   *****************************************************/
  void onGotPrices()
    {
    // Verify that amy promo code was accepted

    String promoCodeInvalidMessage = mOrderPricing.getPromoCodeInvalidMessage();

    if ( promoCodeInvalidMessage != null )
      {
      // A promo code was sent with the request but was invalid.

      // Change the colour to highlight it
      mPromoEditText.setEnabled( true );
      mPromoEditText.setTextColor( getResources().getColor( R.color.payment_promo_code_text_error ) );

      mPromoButton.setText( R.string.payment_promo_button_text_clear );

      mPromoActionClearsCode = true;


      // Note that we show an error message, but we still update the
      // order summary and leave the buttons enabled. That way the
      // user can still pay without the benefit of any promotional
      // discount.

      showErrorDialog( promoCodeInvalidMessage );
      }
    else
      {
      // Either there was no promo code, or it was valid. Save which ever it was.

      mOrder.setPromoCode( mLastSubmittedPromoCode );


      // If there is a promo code - change the text to "Clear" immediately following a retrieval. It
      // will get changed back to "Apply" as soon as the field is changed.

      if ( setPromoButtonEnabledState() )
        {
        mPromoEditText.setEnabled( false );

        mPromoButton.setText( R.string.payment_promo_button_text_clear );

        mPromoActionClearsCode = true;
        }
      else
        {
        mPromoEditText.setEnabled( true );

        mPromoButton.setText( R.string.payment_promo_button_text_apply );

        mPromoActionClearsCode = false;
        }
      }


    // Get the total cost, and save it in the order

    MultipleCurrencyAmount totalCost = mOrderPricing.getTotalCost();

    mOrder.setOrderPricing( mOrderPricing );


    // If the cost is zero, we change the button text
    if ( totalCost.getDefaultAmountWithFallback().getAmount().compareTo( BigDecimal.ZERO ) <= 0 )
      {
      mPayPalButton.setVisibility( View.GONE );

      mCreditCardButton.setText( R.string.payment_credit_card_button_text_free );
      mCreditCardButton.setOnClickListener( new View.OnClickListener()
      {
      @Override
      public void onClick( View view )
        {
        submitOrderForPrinting( null );
        }
      } );
      }
    else
      {
      mPayPalButton.setVisibility( View.VISIBLE );

      mCreditCardButton.setText( R.string.payment_credit_card_button_text );
      }


    OrderPricingAdaptor adaptor = new OrderPricingAdaptor( this, mOrderPricing );

    mOrderSummaryListView.setAdapter( adaptor );
    }


  /*****************************************************
   *
   * Sets the enabled state of the promo button.
   *
   * @return The enabled state.
   *
   *****************************************************/
  private boolean setPromoButtonEnabledState()
    {
    boolean isEnabled = ( mPromoEditText.getText().length() > 0 );

    mPromoButton.setEnabled( isEnabled );

    return ( isEnabled );
    }


  /*****************************************************
   *
   * Called when the promo button is called. It may be
   * in one of two states:
   *   - Apply
   *   - Clear
   *
   *****************************************************/
  public void onPromoButtonClicked( View view )
    {
    onPerformPromoAction();
    }


  /*****************************************************
   *
   * Called when the promo button is called. It may be
   * in one of two states:
   *   - Apply
   *   - Clear
   *
   *****************************************************/
  public void onPerformPromoAction()
    {
    if ( mPromoActionClearsCode )
      {
      mPromoEditText.setEnabled( true );
      mPromoEditText.setText( null );

      mPromoButton.setText( R.string.payment_promo_button_text_apply );
      mPromoButton.setEnabled( false );

      mPromoActionClearsCode = false;


      // If we are clearing a promo code that was successfully used - re-request the
      // prices (i.e. without the code).

      if ( mLastSubmittedPromoCode != null && mLastPriceRetrievalSucceeded )
        {
        requestPrices();
        }
      }
    else
      {
      hideKeyboardDelayed();

      requestPrices();
      }
    }


  /*****************************************************
   *
   * Called when the pay by PayPal button is clicked.
   *
   *****************************************************/
  public void onPayPalButtonClicked( View view )
    {
    if ( mOrderPricing != null )
      {
      MultipleCurrencyAmount multipleCurrencyTotalCost = mOrderPricing.getTotalCost();

      if ( multipleCurrencyTotalCost != null )
        {
        SingleCurrencyAmount totalCost = multipleCurrencyTotalCost.getDefaultAmountWithFallback();

        // TODO: See if we can remove the credit card payment option
        PayPalPayment payment = new PayPalPayment(
                totalCost.getAmount(),
                totalCost.getCurrencyCode(),
                "Product",
                PayPalPayment.PAYMENT_INTENT_SALE );

        Intent intent = new Intent( this, com.paypal.android.sdk.payments.PaymentActivity.class );

        intent.putExtra( com.paypal.android.sdk.payments.PaymentActivity.EXTRA_PAYMENT, payment );

        startActivityForResult( intent, REQUEST_CODE_PAYPAL );
        }
      }
    }


  /*****************************************************
   *
   * Called when the pay by credit card button is clicked.
   *
   *****************************************************/
  public void onCreditCardButtonClicked( View view )
    {
    // Check if a different credit card fragment has been declared

    String creditCardFragmentClassName = getString( R.string.credit_card_fragment_class_name );

    if ( creditCardFragmentClassName != null && ( ! creditCardFragmentClassName.trim().equals( "" ) ) )
      {
      payWithExternalCardFragment( creditCardFragmentClassName );

      return;
      }


    final PayPalCard lastUsedCard = PayPalCard.getLastUsedCard( this );
    if ( lastUsedCard != null && !lastUsedCard.hasVaultStorageExpired() )
      {
      AlertDialog.Builder builder = new AlertDialog.Builder( this );

      if ( mKiteSDKEnvironment.getPayPalEnvironment().equals( PayPalConfiguration.ENVIRONMENT_SANDBOX ) )
        {
        builder.setTitle( "Payment Source (Sandbox)" );
        }
      else
        {
        builder.setTitle( "Payment Source" );
        }

      builder.setItems( new String[]{ "Pay with new card", "Pay with card ending " + lastUsedCard.getLastFour() }, new DialogInterface.OnClickListener()
      {
      @Override
      public void onClick( DialogInterface dialogInterface, int itemIndex )
        {
        if ( itemIndex == 0 )
          {
          payWithNewCard();
          }
        else
          {
          payWithExistingCard( lastUsedCard );
          }
        }
      } );
      builder.show();
      }
    else
      {
      payWithNewCard();
      }
    }


  private void payWithExternalCardFragment( String fragmentClassName )
    {
    try
      {
      Class<?> fragmentClass = Class.forName( fragmentClassName );

      ICreditCardFragment creditCardFragment = (ICreditCardFragment)fragmentClass.newInstance();

      creditCardFragment.display( this );
      }
    catch ( ClassNotFoundException cnfe )
      {
      Log.e( LOG_TAG, "Unable to find external card fragment: " + fragmentClassName, cnfe );
      }
    catch ( InstantiationException ie )
      {
      Log.e( LOG_TAG, "Unable to instantiate external card fragment: " + fragmentClassName, ie );
      }
    catch ( IllegalAccessException iae )
      {
      Log.e( LOG_TAG, "Unable to access external card fragment: " + fragmentClassName, iae );
      }
    catch ( ClassCastException cce )
      {
      Log.e( LOG_TAG, "External card fragment is not an instance of ICreditCardFragment: " + fragmentClassName, cce );
      }
    }


  private void payWithNewCard()
    {
    Intent scanIntent = new Intent( this, CardIOActivity.class );

    scanIntent.putExtra( CardIOActivity.EXTRA_REQUIRE_EXPIRY, true );
    scanIntent.putExtra( CardIOActivity.EXTRA_REQUIRE_CVV, true );
    scanIntent.putExtra( CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false );

    startActivityForResult( scanIntent, REQUEST_CODE_CREDITCARD );
    }


  private void payWithExistingCard( PayPalCard card )
    {
    final ProgressDialog dialog = new ProgressDialog( this );
    dialog.setCancelable( false );
    dialog.setTitle( "Processing" );
    dialog.setMessage( "One moment" );
    dialog.show();

    SingleCurrencyAmount totalCost = mOrderPricing.getTotalCost().getDefaultAmountWithFallback();

    card.chargeCard( mKiteSDKEnvironment,
            totalCost.getAmount(),
            totalCost.getCurrencyCode(),
            "",
            new PayPalCardChargeListener()
            {
            @Override
            public void onChargeSuccess( PayPalCard card, String proofOfPayment )
              {
              dialog.dismiss();
              submitOrderForPrinting( proofOfPayment );
              card.saveAsLastUsedCard( PaymentActivity.this );
              }

            @Override
            public void onError( PayPalCard card, Exception ex )
              {
              dialog.dismiss();
              showErrorDialog( ex.getMessage() );
              }
            } );
    }


  public void submitOrderForPrinting( String paymentId )
    {
    if ( paymentId != null )
      {
      mOrder.setProofOfPayment( paymentId );

      Analytics.getInstance( this ).trackPaymentCompleted( mOrder, Analytics.PAYMENT_METHOD_PAYPAL );
      }


    submitOrder();
    }


  public void submitOrder()
    {
    // TODO: Use a dialog fragment
    final ProgressDialog dialog = new ProgressDialog( this );
    dialog.setCancelable( false );
    dialog.setIndeterminate( false );
    dialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
    dialog.setProgressNumberFormat( null );   // Don't display the "N/100" text
    dialog.setTitle( "Processing" );
    dialog.setMessage( "One moment..." );
    dialog.setMax( 100 );
    dialog.show();


    // Submit the print order

    OrderProgressListener orderProgressListener = new OrderProgressListener( dialog );

    OrderSubmitter orderSubmitter = new OrderSubmitter( this, mOrder, orderProgressListener );

    orderSubmitter.submit();
    }


    ////////// Inner Class(es) //////////

  /*****************************************************
   *
   * A text watcher for the promo code.
   *
   *****************************************************/
  private class PromoCodeTextWatcher implements TextWatcher
    {
    @Override
    public void beforeTextChanged( CharSequence charSequence, int i, int i2, int i3 )
      {
      // Ignore
      }

    @Override
    public void onTextChanged( CharSequence charSequence, int i, int i2, int i3 )
      {
      // Ignore
      }

    @Override
    public void afterTextChanged( Editable editable )
      {
      // Clear any error colour on the text
      mPromoEditText.setTextColor( getResources().getColor( R.color.payment_promo_code_text_default ) );

      // Set the enabled state
      setPromoButtonEnabledState();

      // Change the button text back to Apply (even if we disable the button because the code is blank)
      mPromoButton.setText( R.string.payment_promo_button_text_apply );

      mPromoActionClearsCode = false;
      }
    }


  /*****************************************************
   *
   * Starts pricing retrieval.
   *
   *****************************************************/
  private class RetrievePricingRunnable implements Runnable
    {
    @Override
    public void run()
      {
      requestPrices();
      }
    }


  /*****************************************************
   *
   * Listens to progress updates from order submission
   * and polling.
   *
   *****************************************************/
  private class OrderProgressListener implements OrderSubmitter.IProgressListener
    {
    private ProgressDialog  mProgressDialog;


    OrderProgressListener( ProgressDialog progressDialog )
      {
      mProgressDialog = progressDialog;
      }


    /*****************************************************
     *
     * Called with order submission progress.
     *
     *****************************************************/
    public void onOrderUpdate( PrintOrder order, OrderState state, int primaryProgressPercent, int secondaryProgressPercent )
      {
      // Determine what the order state is, and set the progress dialog accordingly

      switch ( state )
        {
        case UPLOADING:
          mProgressDialog.setIndeterminate( false );
          mProgressDialog.setProgress( primaryProgressPercent );
          mProgressDialog.setSecondaryProgress( secondaryProgressPercent );
          mProgressDialog.setMessage( getString( R.string.order_submission_message_uploading ) );
          break;

        // The progress bar becomes indeterminate once the images have been uploaded

        case POSTED:
          mProgressDialog.setIndeterminate( true );
          mProgressDialog.setMessage( getString( R.string.order_submission_message_posted ) );
          break;

        case RECEIVED:
          mProgressDialog.setIndeterminate( true );
          mProgressDialog.setMessage( getString( R.string.order_submission_message_received ) );
          break;

        case ACCEPTED:
          mProgressDialog.setIndeterminate( true );
          mProgressDialog.setMessage( getString( R.string.order_submission_message_accepted ) );
          break;

        case VALIDATED:

          // Fall through

        case PROCESSED:

          onOrderSuccess( order );

          break;

        case CANCELLED:

          mProgressDialog.dismiss();

          displayModalDialog
            (
            R.string.alert_dialog_title_order_cancelled,
            R.string.alert_dialog_message_order_cancelled,
            R.string.OK,
            null,
            NO_BUTTON,
            null
            );

          break;
        }
      }


    /*****************************************************
     *
     * Called when there is an error submitting the order.
     *
     *****************************************************/
    public void onOrderError( PrintOrder order, Exception exception )
      {
      mProgressDialog.dismiss();

      displayModalDialog
              (
                      R.string.alert_dialog_title_order_submission_error,
                      exception.getMessage(),
                      R.string.OK,
                      null,
                      NO_BUTTON,
                      null
              );

      // We no longer seem to have a route into the receipt screen on error
      //OrderReceiptActivity.startForResult( PaymentActivity.this, order, REQUEST_CODE_RECEIPT );
      }


    @Override
    public void onOrderDuplicate( PrintOrder order, String originalOrderId )
      {
      // We do need to replace any order id with the original one
      order.setReceipt( originalOrderId );


      // A duplicate is treated in the same way as a successful submission, since it means
      // the proof of payment has already been accepted and processed.

      onOrderSuccess( order );
      }


    @Override
    public void onOrderTimeout( PrintOrder order )
      {
      mProgressDialog.dismiss();

      displayModalDialog
        (
        R.string.alert_dialog_title_order_timeout,
        R.string.alert_dialog_message_order_timeout,
        R.string.order_timeout_button_wait,
        new SubmitOrderRunnable(),
        R.string.order_timeout_button_give_up,
        null
        );
      }


    /*****************************************************
     *
     * Proceeds to the receipt screen.
     *
     *****************************************************/
    private void onOrderSuccess( PrintOrder order )
      {
      mProgressDialog.dismiss();

      Analytics.getInstance( PaymentActivity.this ).trackOrderSubmission( order );

      OrderReceiptActivity.startForResult( PaymentActivity.this, order, REQUEST_CODE_RECEIPT );
      }

    }


  /*****************************************************
   *
   * Submits the order.
   *
   *****************************************************/
  private class SubmitOrderRunnable implements Runnable
    {
    @Override
    public void run()
      {
      submitOrder();
      }
    }

  }
