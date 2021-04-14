package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

/**
 * Aksjonspunkter for søknad om engangsstønad for fødsel
 */
abstract class AksjonspunktUtlederForFødsel implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    protected FamilieHendelseTjeneste familieHendelseTjeneste;


    AksjonspunktUtlederForFødsel(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    AksjonspunktUtlederForFødsel() {
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) { // NOSONAR Metode rendrer flytdia.
        final var familieHendelseGrunnlag = familieHendelseTjeneste.hentAggregat(param.getBehandlingId());

        // Sjekk om registrert eller allerede overstyrt fødsel. Deretter om frist utløpt
        if (erFødselenRegistrertITps(familieHendelseGrunnlag) == JA) {
            return samsvarerAntallBarnISøknadMedAntallBarnITps(familieHendelseGrunnlag) == NEI ?
                opprettListeForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL) : INGEN_AKSJONSPUNKTER;
        }
        if (finnesOverstyrtFødsel(familieHendelseGrunnlag) == JA) {
            return INGEN_AKSJONSPUNKTER;
        }
        if (erFristForRegistreringAvFødselPassert(familieHendelseGrunnlag) == JA) {
            return opprettListeForAksjonspunkt(SJEKK_MANGLENDE_FØDSEL);
        }
        // Vent på registrering - vurder om det er riktig for FP
        if (harSøkerOppgittFødselISøknad(familieHendelseGrunnlag) == JA) {
            return singletonList(opprettForAksjonspunktMedFrist(AUTO_VENT_PÅ_FØDSELREGISTRERING, Venteårsak.UDEFINERT, utledVentefrist(familieHendelseGrunnlag)));
        }
        // Vurder tilbakemeldinger her (skrivefeil etc)
        if (finnesOverstyrtTermin(familieHendelseGrunnlag) == JA) {
            return INGEN_AKSJONSPUNKTER;
        }
        // Ytelsesspesifikk håndtering av søknad på termin
        return utledAksjonspunkterForTerminbekreftelse(param);
    }

    Utfall erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(AksjonspunktUtlederInput param) {
        return harArbeidsforholdMedArbeidstyperSomAngitt(param) ?  JA : NEI;
    }

    Utfall samsvarerAntallBarnISøknadMedAntallBarnITps(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getSøknadVersjon().getAntallBarn().equals(grunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0)) ?
            JA : NEI;
    }

    Utfall erFristForRegistreringAvFødselPassert(FamilieHendelseGrunnlagEntitet grunnlag) {
        return familieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(grunnlag) ? JA : NEI;
    }

    LocalDateTime utledVentefrist(FamilieHendelseGrunnlagEntitet grunnlag) {
        var venteFrist = grunnlag.getSøknadVersjon().getBarna().stream()
            .map(barn -> barn.getFødselsdato().plusDays(14))
            .findFirst()
            .orElse(LocalDate.now());
        return LocalDateTime.of(venteFrist, LocalDateTime.now().toLocalTime());
    }

    Utfall harSøkerOppgittFødselISøknad(FamilieHendelseGrunnlagEntitet grunnlag) {
        return FamilieHendelseType.TERMIN.equals(grunnlag.getSøknadVersjon().getType()) || grunnlag.getSøknadVersjon().getBarna().isEmpty() ? NEI : JA;
    }

    Utfall erFødselenRegistrertITps(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return familieHendelseGrunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList()).isEmpty() ? NEI : JA;
    }

    Utfall finnesOverstyrtFødsel(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return familieHendelseGrunnlag.getOverstyrtVersjon()
            .filter(fh -> FamilieHendelseType.FØDSEL.equals(fh.getType()))
            .map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList()).isEmpty() ? NEI : JA;
    }

    Utfall finnesOverstyrtTermin(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return familieHendelseGrunnlag.getOverstyrtVersjon()
            .filter(fh -> FamilieHendelseType.TERMIN.equals(fh.getType()))
            .isEmpty() ? NEI : JA;
    }

    private boolean harArbeidsforholdMedArbeidstyperSomAngitt(AksjonspunktUtlederInput param) {
        var behandlingId = param.getBehandlingId();
        var skjæringstidspunkt = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var aktørId = param.getAktørId();
        var grunnlagOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        if (!grunnlagOpt.isPresent()) {
            return false;
        }
        var grunnlag = grunnlagOpt.get();
        var stp = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt);

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId))
                .før(skjæringstidspunkt);

        return !harIngenArbeidsforholdMedLøpendeAktivitetsavtale(filter, stp);
    }

    private boolean harIngenArbeidsforholdMedLøpendeAktivitetsavtale(YrkesaktivitetFilter filter, DatoIntervallEntitet skjæringstidspunkt) {
        return filter.getAnsettelsesPerioder().stream()
            .noneMatch(aa -> aa.getErLøpende() || aa.getPeriode().overlapper(skjæringstidspunkt));
    }

    protected abstract List<AksjonspunktResultat> utledAksjonspunkterForTerminbekreftelse(AksjonspunktUtlederInput param);

}
