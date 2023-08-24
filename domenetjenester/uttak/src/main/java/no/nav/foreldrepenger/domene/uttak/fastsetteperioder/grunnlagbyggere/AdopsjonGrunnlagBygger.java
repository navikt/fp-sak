package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Adopsjon;

import java.util.Optional;

@ApplicationScoped
public class AdopsjonGrunnlagBygger {

    public Optional<Adopsjon.Builder> byggGrunnlag(ForeldrepengerGrunnlag fpGrunnlag) {
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        if (familieHendelser.gjelderTerminFødsel()) {
            return Optional.empty();
        }
        var gjeldendeFamilieHendelse = familieHendelser.getGjeldendeFamilieHendelse();
        return Optional.ofNullable(new Adopsjon.Builder()
            .ankomstNorge(gjeldendeFamilieHendelse.getAnkomstNorge().orElse(null))
            .stebarnsadopsjon(gjeldendeFamilieHendelse.erStebarnsadopsjon()));
    }
}
