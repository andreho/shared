package net.andreho.common.interfaces;


/**
 * Created by a.hofmann
 * At 09.07.2014 15:17
 */
@FunctionalInterface
public interface Initializable<I extends Initializable<I>> {
   /**
    * @return <b>true</b> if this instance was already initialized or no initialization is needed, <b>false</b>
    * otherwise. <br/>Default value: <b>true</b>
    */
   default boolean isInitialized() {
      return true;
   }

   /**
    * Initializes the inner structure of this object checking before {@link #isInitialized()} if needed and returns
    * the current instance for call chaining.<br/>
    * Further calls of {@link #isInitialized()} must return <b>true</b>.
    *
    * @return this object
    */
   I initialize();
}
