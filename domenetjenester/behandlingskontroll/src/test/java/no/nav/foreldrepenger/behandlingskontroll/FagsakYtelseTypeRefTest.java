package no.nav.foreldrepenger.behandlingskontroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

/**
 * Demonstrerer lookup med repeatble annotations.
 */
@ExtendWith(CdiAwareExtension.class)
class FagsakYtelseTypeRefTest {

    @Test
    void skal_få_duplikat_instans_av_cdi_bean() {
        assertThatThrownBy(() -> FagsakYtelseTypeRef.Lookup.find(Bokstav.class, FagsakYtelseType.FORELDREPENGER)).isInstanceOf(
            IllegalStateException.class).hasMessageContaining("Har flere matchende instanser");
    }

    @Test
    void skal_få_unik_instans_av_cdi_bean() {
        var instans = FagsakYtelseTypeRef.Lookup.find(Bokstav.class, FagsakYtelseType.SVANGERSKAPSPENGER);
        assertThat(instans).isNotNull();
    }

    public interface Bokstav {
    }

    @ApplicationScoped
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
    public static class A implements Bokstav {

    }

    @ApplicationScoped
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    public static class B implements Bokstav {

    }
}
