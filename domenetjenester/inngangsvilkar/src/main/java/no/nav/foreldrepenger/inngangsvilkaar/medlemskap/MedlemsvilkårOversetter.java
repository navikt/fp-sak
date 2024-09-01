package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.RegelPersonStatusType;

@ApplicationScoped
public class MedlemsvilkårOversetter {

    private static final Map<PersonstatusType, RegelPersonStatusType> MAP_PERSONSTATUS_TYPE = Map.of(
        PersonstatusType.BOSA, RegelPersonStatusType.BOSA,
        PersonstatusType.ADNR, RegelPersonStatusType.BOSA,
        PersonstatusType.UTVA, RegelPersonStatusType.UTVA,
        PersonstatusType.DØD, RegelPersonStatusType.DØD
    );

    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    MedlemsvilkårOversetter() {
        // for CDI proxy
    }

    /**
     * @param tidligsteUtstedelseAvTerminBekreftelse - Periode for tidligst utstedelse av terminbekreftelse før termindato
     */
    @Inject
    public MedlemsvilkårOversetter(BehandlingRepositoryProvider repositoryProvider,
                                   PersonopplysningTjeneste personopplysningTjeneste,
                                   InntektArbeidYtelseTjeneste iayTjeneste) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.iayTjeneste = iayTjeneste;
        this.medlemskapPerioderTjeneste = new MedlemskapPerioderTjeneste();
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public MedlemskapsvilkårGrunnlag oversettTilRegelModellMedlemskap(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var behandlingId = ref.behandlingId();
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        var iayOpt = iayTjeneste.finnGrunnlag(behandlingId);

        var medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);

        var vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);

        // // FP VK 2.13
        var vurdertErMedlem = brukerErMedlemEllerIkkeRelevantPeriode(medlemskap, personopplysninger, stp);
        // FP VK 2.2 Er bruker avklart som pliktig eller frivillig medlem?
        var avklartPliktigEllerFrivillig = erAvklartSomPliktigEllerFrivillingMedlem(medlemskap, stp);
        // defaulter uavklarte fakta til true
        var vurdertBosatt = vurdertMedlemskap.map(VurdertMedlemskap::getBosattVurdering).orElse(true);
        var vurdertLovligOpphold = vurdertMedlemskap.map(VurdertMedlemskap::getLovligOppholdVurdering).orElse(true);
        var vurdertOppholdsrett = vurdertMedlemskap.map(VurdertMedlemskap::getOppholdsrettVurdering).orElse(true);

        var harOppholdstillatelse = personopplysningTjeneste.harOppholdstillatelseForPeriode(ref.behandlingId(), stp.getUttaksintervall().orElse(null));
        var harArbeidInntekt = FinnOmSøkerHarArbeidsforholdOgInntekt.finn(iayOpt, stp.getUtledetSkjæringstidspunkt(), ref.aktørId());

        return new MedlemskapsvilkårGrunnlag(
            tilPersonStatusType(personopplysninger, stp.getUtledetSkjæringstidspunkt()), // FP VK 2.1
            brukerNorskNordisk(personopplysninger, stp.getUtledetSkjæringstidspunkt()), // FP VK 2.11
            brukerBorgerAvEOS(vurdertMedlemskap, personopplysninger, stp.getUtledetSkjæringstidspunkt()), // FP VIK 2.12
            harOppholdstillatelse,
            harArbeidInntekt,
            vurdertErMedlem,
            avklartPliktigEllerFrivillig,
            vurdertBosatt,
            vurdertLovligOpphold,
            vurdertOppholdsrett);
    }

    /**
     * True dersom saksbehandler har vurdert til å være medlem i relevant periode
     */
    private boolean erAvklartSomPliktigEllerFrivillingMedlem(Optional<MedlemskapAggregat> medlemskap, Skjæringstidspunkt skjæringstidspunkter) {
        if (medlemskap.isPresent()) {
            var vurdertMedlemskapOpt = medlemskap.get().getVurdertMedlemskap();
            if (vurdertMedlemskapOpt.isPresent()) {
                var vurdertMedlemskap = vurdertMedlemskapOpt.get();
                if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                    MedlemskapManuellVurderingType.MEDLEM.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                    return true;
                }
                if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                    MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                    return false;
                }
            }
            return medlemskapPerioderTjeneste.brukerMaskineltAvklartSomFrivilligEllerPliktigMedlem(
                medlemskap.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()),
                skjæringstidspunkter.getUtledetSkjæringstidspunkt());
        }
        return false;
    }

    /**
     * True dersom saksbehandler har vurdert til ikke å være medlem i relevant periode
     */
    private static boolean erAvklartSomIkkeMedlem(Optional<VurdertMedlemskap> medlemskap) {
        return medlemskap.isPresent() && medlemskap.get().getMedlemsperiodeManuellVurdering() != null
            && MedlemskapManuellVurderingType.UNNTAK.equals(medlemskap.get().getMedlemsperiodeManuellVurdering());
    }

    private boolean brukerErMedlemEllerIkkeRelevantPeriode(Optional<MedlemskapAggregat> medlemskap, PersonopplysningerAggregat søker,
                                                           Skjæringstidspunkt skjæringstidspunkter) {
        var vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);
        if (vurdertMedlemskap.isPresent()
            && MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.get().getMedlemsperiodeManuellVurdering())) {
            return true;
        }

        Set<MedlemskapPerioderEntitet> medlemskapPerioder = medlemskap.isPresent() ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptySet();
        var erAvklartMaskineltSomIkkeMedlem = medlemskapPerioderTjeneste.brukerMaskineltAvklartSomIkkeMedlem(søker,
            medlemskapPerioder, skjæringstidspunkter.getUtledetSkjæringstidspunkt());
        var erAvklartManueltSomIkkeMedlem = erAvklartSomIkkeMedlem(vurdertMedlemskap);

        return !(erAvklartMaskineltSomIkkeMedlem || erAvklartManueltSomIkkeMedlem);
    }

    private static boolean brukerBorgerAvEOS(Optional<VurdertMedlemskap> medlemskap, PersonopplysningerAggregat aggregat, LocalDate vurderingsdato) {
        // Tar det første for det er det som er prioritert høyest rangert på region
        var eosBorger = aggregat.harStatsborgerskapRegionVedSkjæringstidspunkt(aggregat.getSøker().getAktørId(), Region.EOS, vurderingsdato);
        return medlemskap
            .map(VurdertMedlemskap::getErEøsBorger)
            .orElse(eosBorger);
    }

    private static boolean brukerNorskNordisk(PersonopplysningerAggregat aggregat, LocalDate vurderingsdato) {
        return aggregat.harStatsborgerskapRegionVedSkjæringstidspunkt(aggregat.getSøker().getAktørId(), Region.NORDEN, vurderingsdato);
    }

    private static RegelPersonStatusType tilPersonStatusType(PersonopplysningerAggregat personopplysninger, LocalDate vurderingsdato) {
        // Bruker overstyrt personstatus hvis det finnes
        return Optional.ofNullable(personopplysninger.getPersonstatusFor(personopplysninger.getSøker().getAktørId(), SimpleLocalDateInterval.enDag(vurderingsdato)))
            .map(PersonstatusEntitet::getPersonstatus)
            .map(MAP_PERSONSTATUS_TYPE::get)
            .orElse(null);
    }

}
