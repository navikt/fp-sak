package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.VENT_PÅ_FØDSEL;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingsgrunnlagKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class AvklarOmSøkerOppholderSegINorge {

    private FamilieHendelseRepository familieGrunnlagRepository;
    private SøknadRepository søknadRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingsgrunnlagKodeverkRepository behandlingsgrunnlagKodeverkRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public AvklarOmSøkerOppholderSegINorge(BehandlingRepositoryProvider repositoryProvider,
                                           BehandlingsgrunnlagKodeverkRepository behandlingsgrunnlagKodeverkRepository,
                                           PersonopplysningTjeneste personopplysningTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingsgrunnlagKodeverkRepository = behandlingsgrunnlagKodeverkRepository;
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public Optional<MedlemResultat> utled(BehandlingReferanse ref, LocalDate vurderingstidspunkt) {
        Long behandlingId = ref.getBehandlingId();
        final List<String> landkoder = getLandkode(ref.getBehandlingId(), ref.getAktørId(), vurderingstidspunkt);
        Region region = Region.UDEFINERT;
        if (!landkoder.isEmpty()) {
            region = behandlingsgrunnlagKodeverkRepository.finnHøyestRangertRegion(landkoder);
        }
        if ((harFødselsdato(behandlingId) == JA) || (harDatoForOmsorgsovertakelse(behandlingId) == JA)) {
            return Optional.empty();
        }
        if ((harNordiskStatsborgerskap(region) == JA) || (harAnnetStatsborgerskap(region) == JA)) {
            return Optional.empty();
        }
        if ((erGiftMedNordiskBorger(ref) == JA) || (erGiftMedBorgerMedANNETStatsborgerskap(ref) == JA)) {
            return Optional.empty();
        }
        if (harSøkerHattInntektINorgeDeSiste3Mnd(behandlingId, ref.getAktørId(), vurderingstidspunkt) == JA) {
            return Optional.empty();
        }
        if (harTermindatoPassertMed14Dager(behandlingId) == NEI) {
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

    private Utfall harSøkerHattInntektINorgeDeSiste3Mnd(Long behandlingId, AktørId aktørId, LocalDate vurderingstidspunkt) {
        final SøknadEntitet søknad = søknadRepository.hentSøknad(behandlingId);
        LocalDate mottattDato = søknad.getMottattDato();
        LocalDate treMndTilbake = mottattDato.minusMonths(3L);

        // OBS: ulike regler for vilkår og autopunkt. For EØS-par skal man vente hvis søker ikke har inntekt siste 3mnd.
        Optional<InntektArbeidYtelseGrunnlag> grunnlag = iayTjeneste.finnGrunnlag(behandlingId);

        boolean inntektSiste3M = false;
        if (grunnlag.isPresent()) {
            var filter = new InntektFilter(grunnlag.get().getAktørInntektFraRegister(aktørId)).før(vurderingstidspunkt);
            inntektSiste3M = filter.getInntektsposterPensjonsgivende().stream()
                .anyMatch(ip -> ip.getPeriode().getFomDato().isBefore(vurderingstidspunkt) && ip.getPeriode().getTomDato().isAfter(treMndTilbake));
        }

        return inntektSiste3M ? JA : NEI;
    }

    private Utfall harTermindatoPassertMed14Dager(Long behandlingId) {
        LocalDate dagensDato = LocalDate.now();
        final Optional<LocalDate> termindato = familieGrunnlagRepository.hentAggregat(behandlingId).getGjeldendeTerminbekreftelse()
            .map(TerminbekreftelseEntitet::getTermindato);
        return termindato.filter(localDate -> localDate.plusDays(14L).isBefore(dagensDato)).map(localDate -> JA).orElse(NEI);
    }

    private List<String> getLandkode(Long behandlingId, AktørId aktørId, LocalDate vurderingstidspunkt) {
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandlingId, aktørId,
            vurderingstidspunkt);

        return personopplysninger.getStatsborgerskapFor(aktørId)
            .stream()
            .map(StatsborgerskapEntitet::getStatsborgerskap)
            .map(Landkoder::getKode)
            .collect(Collectors.toList());
    }
}
