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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

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

    public Optional<MedlemResultat> utled(BehandlingReferanse ref, LocalDate vurderingstidspunkt) {
        Long behandlingId = ref.getBehandlingId();
        final Region region = getRegion(ref.getBehandlingId(), ref.getAktørId(), vurderingstidspunkt);
        if ((harFødselsdato(behandlingId) == JA) || (harDatoForOmsorgsovertakelse(behandlingId) == JA)) {
            return Optional.empty();
        }
        if ((harNordiskStatsborgerskap(region) == JA) || (harAnnetStatsborgerskap(region) == JA)) {
            return Optional.empty();
        }
        if ((erGiftMedNordiskBorger(ref) == JA) || (erGiftMedBorgerMedANNETStatsborgerskap(ref) == JA)) {
            return Optional.empty();
        }
        if (harOppholdstilltatelseVed(ref, vurderingstidspunkt) == JA) {
            return Optional.empty();
        }
        if (harSøkerHattInntektINorgeDeSiste3Mnd(ref, vurderingstidspunkt) == JA) {
            return Optional.empty();
        }
        if (!FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.getFagsakYtelseType()) && harTermindatoPassertMed14Dager(behandlingId) == NEI) {
            return Optional.of(VENT_PÅ_FØDSEL);
        }
        return Optional.of(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    private Utfall harFødselsdato(Long behandlingId) {
        final FamilieHendelseGrunnlagEntitet grunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        if (!grunnlag.getGjeldendeBekreftetVersjon().map(FamilieHendelseEntitet::getBarna).map(List::isEmpty).orElse(true)) {
            return JA;
        }
        final FamilieHendelseEntitet søknad = grunnlag.getSøknadVersjon();
        if (!FamilieHendelseType.FØDSEL.equals(søknad.getType())) {
            return NEI;
        }
        return søknad.getBarna().isEmpty() ? NEI : JA;
    }

    private Utfall harDatoForOmsorgsovertakelse(Long behandlingId) {
        final FamilieHendelseGrunnlagEntitet grunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        final FamilieHendelseEntitet søknad = grunnlag.getSøknadVersjon();
        return søknad.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).isPresent() ? JA : NEI;
    }

    private Utfall harNordiskStatsborgerskap(Region region) {
        return Region.NORDEN.equals(region) ? JA : NEI;
    }

    private Utfall harAnnetStatsborgerskap(Region region) {
        return (region == null || Region.TREDJELANDS_BORGER.equals(region)) || Region.UDEFINERT.equals(region) ? JA : NEI;
    }

    private Utfall erGiftMedNordiskBorger(BehandlingReferanse ref) {
        return erGiftMed(ref, Region.NORDEN);
    }

    private Utfall erGiftMedBorgerMedANNETStatsborgerskap(BehandlingReferanse ref) {
        Utfall utfall = erGiftMed(ref, Region.TREDJELANDS_BORGER);
        if (utfall == NEI) {
            utfall = erGiftMed(ref, Region.UDEFINERT);
        }
        return utfall;
    }

    private Utfall erGiftMed(BehandlingReferanse ref, Region region) {
        Optional<PersonopplysningEntitet> ektefelle = personopplysningTjeneste.hentPersonopplysninger(ref).getEktefelle();
        if (ektefelle.isPresent()) {
            if (ektefelle.get().getRegion().equals(region)) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall harSøkerHattInntektINorgeDeSiste3Mnd(BehandlingReferanse ref, LocalDate vurderingstidspunkt) {
        var intervall3mnd = utledInntektsintervall3Mnd(ref, vurderingstidspunkt);

        // OBS: ulike regler for vilkår og autopunkt. For EØS-par skal man vente hvis søker ikke har inntekt siste 3mnd.
        Optional<InntektArbeidYtelseGrunnlag> grunnlag = iayTjeneste.finnGrunnlag(ref.getBehandlingId());

        boolean inntektSiste3M = false;
        if (grunnlag.isPresent()) {
            var filter = new InntektFilter(grunnlag.get().getAktørInntektFraRegister(ref.getAktørId())).før(vurderingstidspunkt);
            inntektSiste3M = filter.getInntektsposterPensjonsgivende().stream()
                .anyMatch(ip -> intervall3mnd.overlapper(ip.getPeriode()));
        }

        return inntektSiste3M ? JA : NEI;
    }

    private DatoIntervallEntitet utledInntektsintervall3Mnd(BehandlingReferanse referanse, LocalDate vurderingstidspunkt) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(referanse.getFagsakYtelseType()) && LocalDate.now().isBefore(vurderingstidspunkt)) {
            final var søknadMottattDato = søknadRepository.hentSøknad(referanse.getBehandlingId()).getMottattDato();
            var brukdato = søknadMottattDato.isBefore(vurderingstidspunkt) ? søknadMottattDato : vurderingstidspunkt;
            return DatoIntervallEntitet.fraOgMedTilOgMed(brukdato.minusMonths(3), brukdato);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(vurderingstidspunkt.minusMonths(3), vurderingstidspunkt);
    }


    private Utfall harOppholdstilltatelseVed(BehandlingReferanse ref, LocalDate vurderingsdato) {
        if (ref.getUtledetMedlemsintervall().encloses(vurderingsdato)) {
            return personopplysningTjeneste.harOppholdstillatelseForPeriode(ref.getBehandlingId(), ref.getUtledetMedlemsintervall()) ? JA : NEI;
        }
        return personopplysningTjeneste.harOppholdstillatelsePåDato(ref.getBehandlingId(), vurderingsdato) ? JA : NEI;
    }

    private Utfall harTermindatoPassertMed14Dager(Long behandlingId) {
        LocalDate dagensDato = LocalDate.now();
        final Optional<LocalDate> termindato = familieGrunnlagRepository.hentAggregat(behandlingId).getGjeldendeTerminbekreftelse()
            .map(TerminbekreftelseEntitet::getTermindato);
        return termindato.filter(localDate -> localDate.plusDays(14L).isBefore(dagensDato)).map(localDate -> JA).orElse(NEI);
    }

    private Region getRegion(Long behandlingId, AktørId aktørId, LocalDate vurderingstidspunkt) {
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandlingId, aktørId,
            vurderingstidspunkt);

        return personopplysninger.getStatsborgerskapRegionVedTidspunkt(aktørId, vurderingstidspunkt);
    }
}
