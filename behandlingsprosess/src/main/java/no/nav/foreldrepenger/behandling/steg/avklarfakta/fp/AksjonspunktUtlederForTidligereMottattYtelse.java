package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.YtelserKonsolidertTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.TilgrensendeYtelserDto;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
class AksjonspunktUtlederForTidligereMottattYtelse implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();

    private static final Set<RelatertYtelseType> RELEVANTE_YTELSE_TYPER = Set.of(RelatertYtelseType.FORELDREPENGER, RelatertYtelseType.ENGANGSSTØNAD);
    private static final int ANTALL_MÅNEDER = 10; // 10 mnd fra LB for sjekk med tom. vurder 11/12 hvis sjekk på fom.

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private YtelserKonsolidertTjeneste ytelseTjeneste;

    // For CDI.
    AksjonspunktUtlederForTidligereMottattYtelse() {
    }

    @Inject
    AksjonspunktUtlederForTidligereMottattYtelse(InntektArbeidYtelseTjeneste iayTjeneste, YtelserKonsolidertTjeneste ytelseTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.ytelseTjeneste = ytelseTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var behandlingId = param.getBehandlingId();
        var skjæringstidspunkt = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();

        var inntektArbeidYtelseGrunnlag = iayTjeneste.finnGrunnlag(behandlingId);
        if (!inntektArbeidYtelseGrunnlag.isPresent()) {
            return INGEN_AKSJONSPUNKTER;
        }
        var grunnlag = inntektArbeidYtelseGrunnlag.get();
        if (harMottattStønadSiste10Mnd(param.getSaksnummer(), param.getAktørId(), grunnlag, skjæringstidspunkt) == JA) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
        }

        // TODO: Vurder behov for inntektssjekk for å dekke flyttetilfelle. Gjort for ES
        // i Fundamentet og her
        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall harMottattStønadSiste10Mnd(Saksnummer saksnummer, AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag,
            LocalDate skjæringstidspunkt) {
        var vedtakEtterDato = skjæringstidspunkt.minusMonths(ANTALL_MÅNEDER);
        var ytelser = ytelseTjeneste.utledYtelserRelatertTilBehandling(aktørId, grunnlag,
                Optional.of(RELEVANTE_YTELSE_TYPER));
        Boolean senerevedtak = ytelser.stream()
                .filter(y -> (y.getSaksNummer() == null) || !saksnummer.getVerdi().equals(y.getSaksNummer()))
                .map(TilgrensendeYtelserDto::getPeriodeFraDato)
                .anyMatch(vedtakEtterDato::isBefore);
        return senerevedtak ? JA : NEI;
    }
}
