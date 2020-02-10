package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Adopsjon;

@ApplicationScoped
public class AdopsjonGrunnlagBygger {

    public Optional<Adopsjon.Builder> byggGrunnlag(ForeldrepengerGrunnlag fpGrunnlag) {
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        if (familieHendelser.gjelderTerminFødsel()) {
            return Optional.empty();
        }
        var gjeldendeFamilieHendelse = familieHendelser.getGjeldendeFamilieHendelse();
        return Optional.ofNullable(new Adopsjon.Builder()
            .medAnkomstNorge(gjeldendeFamilieHendelse.getAnkomstNorge().orElse(null))
            .medStebarnsadopsjon(gjeldendeFamilieHendelse.erStebarnsadopsjon()));
    }
}
