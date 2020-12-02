package no.nav.foreldrepenger.behandlingskontroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

/**
 * Demonstrerer lookup med repeatble annotations.
 */
@CdiDbAwareTest
public class FagsakYtelseTypeRefTest {

    @Test
    public void skal_få_duplikat_instans_av_cdi_bean() {
        assertThatThrownBy(
                () -> FagsakYtelseTypeRef.Lookup.find(Bokstav.class, FagsakYtelseType.FORELDREPENGER)).isInstanceOf(
                        IllegalStateException.class).hasMessageContaining("Har flere matchende instanser");
    }

    @Test
    public void skal_få_unik_instans_av_cdi_bean() {
        var instans = FagsakYtelseTypeRef.Lookup.find(Bokstav.class, FagsakYtelseType.SVANGERSKAPSPENGER);
        assertThat(instans).isNotNull();
    }

    public interface Bokstav {
    }

    @ApplicationScoped
    @FagsakYtelseTypeRef("FP")
    @FagsakYtelseTypeRef("SVP")
    public static class A implements Bokstav {

    }

    @ApplicationScoped
    @FagsakYtelseTypeRef("FP")
    public static class B implements Bokstav {

    }
}
