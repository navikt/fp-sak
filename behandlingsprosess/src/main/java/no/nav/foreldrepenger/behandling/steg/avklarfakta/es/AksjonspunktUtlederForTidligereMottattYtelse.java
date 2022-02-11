package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
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
    private static final int ANTALL_MÅNEDER = 10; // 10 mnd fra LB. Vurder 11-12 hvis sjekk på ytelse.fom

    private PersonopplysningRepository personopplysningRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private YtelserKonsolidertTjeneste ytelseTjeneste;

    // For CDI.
    AksjonspunktUtlederForTidligereMottattYtelse() {
    }

    @Inject
    AksjonspunktUtlederForTidligereMottattYtelse(InntektArbeidYtelseTjeneste iayTjeneste, YtelserKonsolidertTjeneste ytelseTjeneste,
            PersonopplysningRepository personopplysningRepository) {
        this.iayTjeneste = iayTjeneste;
        this.personopplysningRepository = personopplysningRepository;
        this.ytelseTjeneste = ytelseTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var behandlingId = param.getBehandlingId();
        var aktørId = param.getAktørId();

        var skjæringstidspunkt = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();

        var inntektArbeidYtelseGrunnlagOpt = iayTjeneste.finnGrunnlag(behandlingId);
        if (!inntektArbeidYtelseGrunnlagOpt.isPresent()) {
            return INGEN_AKSJONSPUNKTER;
        }
        var grunnlag = inntektArbeidYtelseGrunnlagOpt.get();
        if (harMottattStønadSiste10Mnd(param.getSaksnummer(), param.getAktørId(), grunnlag, skjæringstidspunkt) == JA) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
        }

        var annenPart = finnOppgittAnnenPart(behandlingId).orElse(null);
        if (annenPart != null && harAnnenPartMottattStønadSiste10Mnd(param.getSaksnummer(), annenPart, grunnlag, skjæringstidspunkt) == JA) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE);
        }

        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall harMottattStønadSiste10Mnd(Saksnummer saksnummer, AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, LocalDate skjæringstidspunkt) {
        var vedtakEtterDato = skjæringstidspunkt.minusMonths(ANTALL_MÅNEDER);
        var ytelser = ytelseTjeneste.utledYtelserRelatertTilBehandling(aktørId, grunnlag, RELEVANTE_YTELSE_TYPER);
        var senerevedtak = ytelser.stream()
                .filter(y -> (y.getSaksNummer() == null) || !saksnummer.getVerdi().equals(y.getSaksNummer()))
                .map(TilgrensendeYtelserDto::getPeriodeFraDato)
                .anyMatch(vedtakEtterDato::isBefore);
        return senerevedtak ? JA : NEI;
    }

    private Utfall harAnnenPartMottattStønadSiste10Mnd(@SuppressWarnings("unused") Saksnummer saksnummer, AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, LocalDate skjæringstidspunkt) {
        var vedtakEtterDato = skjæringstidspunkt.minusMonths(ANTALL_MÅNEDER);
        var ytelser = ytelseTjeneste.utledAnnenPartsYtelserRelatertTilBehandling(aktørId, grunnlag, RELEVANTE_YTELSE_TYPER);
        var senerevedtak = ytelser.stream().map(y -> y.getPeriodeTilDato() != null ? y.getPeriodeTilDato() : y.getPeriodeFraDato()).anyMatch(vedtakEtterDato::isBefore);
        return senerevedtak ? JA : NEI;
    }

    private Optional<AktørId> finnOppgittAnnenPart(Long behandlingId) {
        var personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);
        return personopplysningGrunnlag.getOppgittAnnenPart().map(OppgittAnnenPartEntitet::getAktørId);
    }
}
