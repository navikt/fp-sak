package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Adopsjon;

public class AdopsjonGrunnlagByggerTest {

    @Test
    public void skalLeggeTilAdopsjonInformasjon_IngenAnkomstNorgeDatoEllerStebarnsadopsjon() {
        Adopsjon grunnlag = byggGrunnlag(fpGrunnlag(null, false));

        assertThat(grunnlag.getAnkomstNorgeDato()).isNull();
        assertThat(grunnlag.erStebarnsadopsjon()).isFalse();
    }

    private ForeldrepengerGrunnlag fpGrunnlag(LocalDate ankomstNorge, boolean stebarnsadopsjon) {
        var bekreftetFamilieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(LocalDate.now(), List.of(new Barn()), 1, ankomstNorge, stebarnsadopsjon);
        FamilieHendelser familieHendelser = new FamilieHendelser().medBekreftetHendelse(bekreftetFamilieHendelse);
        return new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser);
    }

    @Test
    public void skalLeggeTilAdopsjonInformasjon_IngenAnkomstNorgeDatoOgStebarnsadopsjonTrue() {
        Adopsjon grunnlag = byggGrunnlag(fpGrunnlag(null, true));

        assertThat(grunnlag.getAnkomstNorgeDato()).isNull();
        assertThat(grunnlag.erStebarnsadopsjon()).isTrue();
    }

    @Test
    public void skalLeggeTilAdopsjonInformasjon_AnkomstNorgeDatoOgStebarnsadopsjonTrue() {
        LocalDate ankomstNorgeDato = LocalDate.now().plusWeeks(2);
        Adopsjon grunnlag = byggGrunnlag(fpGrunnlag(ankomstNorgeDato, true));

        assertThat(grunnlag.getAnkomstNorgeDato()).isEqualTo(ankomstNorgeDato);
        assertThat(grunnlag.erStebarnsadopsjon()).isTrue();
    }

    @Test
    public void skalLeggeTilAdopsjonInformasjon_AnkomstNorgeDatoOgStebarnsadopsjonFalse() {
        LocalDate ankomstNorgeDato = LocalDate.now().plusWeeks(2);
        Adopsjon grunnlag = byggGrunnlag(fpGrunnlag(ankomstNorgeDato, false));

        assertThat(grunnlag.getAnkomstNorgeDato()).isEqualTo(ankomstNorgeDato);
        assertThat(grunnlag.erStebarnsadopsjon()).isFalse();
    }

    private AdopsjonGrunnlagBygger grunnlagBygger() {
        return new AdopsjonGrunnlagBygger();
    }

    private Adopsjon byggGrunnlag(ForeldrepengerGrunnlag fpGrunnlag) {
        return grunnlagBygger().byggGrunnlag(fpGrunnlag).orElseThrow().build();
    }

}
