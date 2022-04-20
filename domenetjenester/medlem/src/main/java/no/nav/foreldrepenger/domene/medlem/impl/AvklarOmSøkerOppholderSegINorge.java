package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.VENT_PÅ_FØDSEL;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class AvklarOmSøkerOppholderSegINorge {

    private FamilieHendelseRepository familieGrunnlagRepository;
    private SøknadRepository søknadRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public AvklarOmSøkerOppholderSegINorge(BehandlingRepositoryProvider repositoryProvider,
                                           PersonopplysningTjeneste personopplysningTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public Optional<MedlemResultat> utledVedSTP(BehandlingReferanse ref) {
        var vurderingstidspunkt = ref.getUtledetSkjæringstidspunkt();
        var behandlingId = ref.behandlingId();
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        final var region = getRegion(ref, personopplysninger);
        if ((harFødselsdato(behandlingId) == JA) || (harDatoForOmsorgsovertakelse(behandlingId) == JA)) {
            return Optional.empty();
        }
        if ((harNordiskStatsborgerskap(region) == JA) || (harAnnetStatsborgerskap(region) == JA)) {
            return Optional.empty();
        }
        if ((erGiftMedNordiskBorger(personopplysninger) == JA) || (erGiftMedBorgerMedANNETStatsborgerskap(personopplysninger) == JA)) {
            return Optional.empty();
        }
        if (harOppholdstilltatelseVed(ref, vurderingstidspunkt) == JA) {
            return Optional.empty();
        }
        if (harSøkerHattInntektINorgeDeSiste3Mnd(ref, vurderingstidspunkt) == JA) {
            return Optional.empty();
        }
        if (!FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType()) && harTermindatoPassertMed14Dager(behandlingId) == NEI) {
            return Optional.of(VENT_PÅ_FØDSEL);
        }
        return Optional.of(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    private Utfall harFødselsdato(Long behandlingId) {
        final var grunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        if (!grunnlag.getGjeldendeBekreftetVersjon().map(FamilieHendelseEntitet::getBarna).map(List::isEmpty).orElse(true)) {
            return JA;
        }
        final var søknad = grunnlag.getSøknadVersjon();
        if (!FamilieHendelseType.FØDSEL.equals(søknad.getType())) {
            return NEI;
        }
        return søknad.getBarna().isEmpty() ? NEI : JA;
    }

    private Utfall harDatoForOmsorgsovertakelse(Long behandlingId) {
        final var grunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        final var søknad = grunnlag.getSøknadVersjon();
        return søknad.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).isPresent() ? JA : NEI;
    }

    private Utfall harNordiskStatsborgerskap(Region region) {
        return Region.NORDEN.equals(region) ? JA : NEI;
    }

    private Utfall harAnnetStatsborgerskap(Region region) {
        return (region == null || Region.TREDJELANDS_BORGER.equals(region)) || Region.UDEFINERT.equals(region) ? JA : NEI;
    }

    private Utfall erGiftMedNordiskBorger(PersonopplysningerAggregat personopplysninger) {
        return erGiftMed(personopplysninger, Region.NORDEN);
    }

    private Utfall erGiftMedBorgerMedANNETStatsborgerskap(PersonopplysningerAggregat personopplysninger) {
        var utfall = erGiftMed(personopplysninger, Region.TREDJELANDS_BORGER);
        if (utfall == NEI) {
            utfall = erGiftMed(personopplysninger, Region.UDEFINERT);
        }
        return utfall;
    }

    private Utfall erGiftMed(PersonopplysningerAggregat personopplysninger, Region region) {
        var ektefelleHarRegion = personopplysninger.getEktefelle()
            .map(e -> personopplysninger.harStatsborgerskapRegionVedSkjæringstidspunkt(e.getAktørId(), region)).orElse(Boolean.FALSE);
        if (ektefelleHarRegion) {
            return JA;
        }
        return NEI;
    }

    private Utfall harSøkerHattInntektINorgeDeSiste3Mnd(BehandlingReferanse ref, LocalDate vurderingstidspunkt) {
        var intervall3mnd = utledInntektsintervall3Mnd(ref, vurderingstidspunkt);

        // OBS: ulike regler for vilkår og autopunkt. For EØS-par skal man vente hvis søker ikke har inntekt siste 3mnd.
        var grunnlag = iayTjeneste.finnGrunnlag(ref.behandlingId());

        var inntektSiste3M = false;
        if (grunnlag.isPresent()) {
            var filter = new InntektFilter(grunnlag.get().getAktørInntektFraRegister(ref.aktørId())).før(vurderingstidspunkt);
            inntektSiste3M = filter.getInntektsposterPensjonsgivende().stream()
                .anyMatch(ip -> intervall3mnd.overlapper(ip.getPeriode()));
        }

        return inntektSiste3M ? JA : NEI;
    }

    private DatoIntervallEntitet utledInntektsintervall3Mnd(BehandlingReferanse referanse, LocalDate vurderingstidspunkt) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(referanse.fagsakYtelseType()) && LocalDate.now().isBefore(vurderingstidspunkt)) {
            final var søknadMottattDato = søknadRepository.hentSøknad(referanse.behandlingId()).getMottattDato();
            var brukdato = søknadMottattDato.isBefore(vurderingstidspunkt) ? søknadMottattDato : vurderingstidspunkt;
            return DatoIntervallEntitet.fraOgMedTilOgMed(brukdato.minusMonths(3), brukdato);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(vurderingstidspunkt.minusMonths(3), vurderingstidspunkt);
    }


    private Utfall harOppholdstilltatelseVed(BehandlingReferanse ref, LocalDate vurderingsdato) {
        if (ref.getUtledetMedlemsintervall().encloses(vurderingsdato)) {
            return personopplysningTjeneste.harOppholdstillatelseForPeriode(ref.behandlingId(), ref.getUtledetMedlemsintervall()) ? JA : NEI;
        }
        return personopplysningTjeneste.harOppholdstillatelsePåDato(ref.behandlingId(), vurderingsdato) ? JA : NEI;
    }

    private Utfall harTermindatoPassertMed14Dager(Long behandlingId) {
        var dagensDato = LocalDate.now();
        final var termindato = familieGrunnlagRepository.hentAggregat(behandlingId).getGjeldendeTerminbekreftelse()
            .map(TerminbekreftelseEntitet::getTermindato);
        return termindato.filter(localDate -> localDate.plusDays(14L).isBefore(dagensDato)).map(localDate -> JA).orElse(NEI);
    }

    private Region getRegion(BehandlingReferanse ref, PersonopplysningerAggregat personopplysninger) {
        return personopplysninger.getStatsborgerskapRegionVedSkjæringstidspunkt(ref.aktørId());
    }
}
