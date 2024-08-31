package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.RegelPersonStatusType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class VurderLøpendeMedlemskap {

    private static final Map<PersonstatusType, RegelPersonStatusType> MAP_PERSONSTATUS_TYPE = Map.of(
        PersonstatusType.BOSA, RegelPersonStatusType.BOSA,
        PersonstatusType.ADNR, RegelPersonStatusType.BOSA,
        PersonstatusType.UTVA, RegelPersonStatusType.UTVA,
        PersonstatusType.DØD, RegelPersonStatusType.DØD
    );

    private PersonopplysningTjeneste personopplysningTjeneste;
    private MedlemskapRepository medlemskapRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private MedlemskapPerioderTjeneste medTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerMedlemskap;

    VurderLøpendeMedlemskap() {
        //CDI
    }

    @Inject
    public VurderLøpendeMedlemskap(PersonopplysningTjeneste personopplysningTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   MedlemskapRepository medlemskapRepository,
                                   MedlemskapPerioderTjeneste medTjeneste,
                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                   UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerMedlemskapTjeneste,
                                   InntektArbeidYtelseTjeneste iayTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.medlemskapRepository = medlemskapRepository;
        this.medTjeneste = medTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.utledVurderingsdatoerMedlemskap = utledVurderingsdatoerMedlemskapTjeneste;
    }

    public Map<LocalDate, VilkårData> vurderLøpendeMedlemskap(Long behandlingId) {
        Map<LocalDate, VilkårData> resultat = new TreeMap<>();

        for (var entry : lagGrunnlag(behandlingId).entrySet()) {
            var data = evaluerGrunnlag(entry.getValue());
            if (data.utfallType().equals(VilkårUtfallType.OPPFYLT)) {
                resultat.put(entry.getKey(), data);
            } else if (data.utfallType().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                if (data.vilkårUtfallMerknad() == null) {
                    throw new IllegalStateException("Forventer at vilkår utfall merknad er satt når vilkåret blir satt til IKKE_OPPFYLT for grunnlag:" + entry.getValue().toString());
                }
                resultat.put(entry.getKey(), data);
                break;
            }
        }
        return resultat;
    }

    private VilkårData evaluerGrunnlag(MedlemskapsvilkårGrunnlag grunnlag) {
        var resultat = InngangsvilkårRegler.medlemskap(grunnlag);
        return RegelResultatOversetter.oversett(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, resultat);
    }

    private Map<LocalDate, MedlemskapsvilkårGrunnlag> lagGrunnlag(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        var medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        var vurdertMedlemskapPeriode = medlemskap.flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap);
        var vurderingsdatoerListe = utledVurderingsdatoerMedlemskap.finnVurderingsdatoer(ref)
            .stream()
            .sorted(LocalDate::compareTo)
            .toList();

        if (vurderingsdatoerListe.isEmpty()) {
            return Collections.emptyMap();
        }

        var map = mapVurderingFraSaksbehandler(vurdertMedlemskapPeriode);

        Map<LocalDate, MedlemskapsvilkårGrunnlag> resulatat = new TreeMap<>();
        for (var vurderingsdato : vurderingsdatoerListe) {
            var vurdertOpt = Optional.ofNullable(map.get(vurderingsdato));
            var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);

            // // FP VK 2.13
            var vurdertErMedlem = brukerErMedlemEllerIkkeRelevantPeriode(medlemskap, vurdertOpt, personopplysningerAggregat, vurderingsdato);
            // FP VK 2.2 Er bruker avklart som pliktig eller frivillig medlem?
            var avklartPliktigEllerFrivillig = erAvklartSomPliktigEllerFrivillingMedlem(vurdertOpt, medlemskap, vurderingsdato);
            // defaulter uavklarte fakta til true
            var vurdertBosatt = vurdertOpt.map(v -> defaultValueTrue(v.getBosattVurdering())).orElse(true);
            var vurdertLovligOpphold = vurdertOpt.map(v -> defaultValueTrue(v.getLovligOppholdVurdering())).orElse(true);
            var vurdertOppholdsrett = vurdertOpt.map(v -> defaultValueTrue(v.getOppholdsrettVurdering())).orElse(true);

            var grunnlag = new MedlemskapsvilkårGrunnlag(
                tilPersonStatusType(personopplysningerAggregat, vurderingsdato), // FP VK 2.1
                brukerNorskNordisk(personopplysningerAggregat, vurderingsdato, ref.getUtledetSkjæringstidspunkt()), // FP VK 2.11
                vurdertOpt.map(v -> defaultValueTrue(v.getErEøsBorger())).orElse(true), // FP VIK 2.12
                harOppholdstillatelsePåDato(ref, vurderingsdato),
                finnOmSøkerHarArbeidsforholdOgInntekt(behandling, vurderingsdato),
                vurdertErMedlem,
                avklartPliktigEllerFrivillig,
                vurdertBosatt,
                vurdertLovligOpphold,
                vurdertOppholdsrett);

            resulatat.put(vurderingsdato, grunnlag);
        }
        return resulatat;
    }

    private Boolean defaultValueTrue(Boolean verdi) {
        if (verdi == null) {
            return true;
        }
        return verdi;
    }

    private Map<LocalDate, VurdertLøpendeMedlemskapEntitet> mapVurderingFraSaksbehandler(Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskapPeriode) {
        Map<LocalDate, VurdertLøpendeMedlemskapEntitet> vurderingFraSaksbehandler = new HashMap<>();
        vurdertMedlemskapPeriode.ifPresent(v -> {
            var perioder = v.getPerioder();
            for (var vurdertLøpendeMedlemskap : perioder) {
                vurderingFraSaksbehandler.put(vurdertLøpendeMedlemskap.getVurderingsdato(), vurdertLøpendeMedlemskap);
            }
        });
        return vurderingFraSaksbehandler;
    }

    private boolean brukerErMedlemEllerIkkeRelevantPeriode(Optional<MedlemskapAggregat> medlemskap, Optional<VurdertLøpendeMedlemskapEntitet> vurdertMedlemskap,
                                                           PersonopplysningerAggregat søker, LocalDate vurderingsdato) {
        if (vurdertMedlemskap.isPresent()
            && MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.get().getMedlemsperiodeManuellVurdering())) {
            return true;
        }

        Set<MedlemskapPerioderEntitet> medlemskapPerioder = medlemskap.isPresent() ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptySet();
        var erAvklartMaskineltSomIkkeMedlem = medTjeneste.brukerMaskineltAvklartSomIkkeMedlem(søker,
            medlemskapPerioder, vurderingsdato);
        var erAvklartManueltSomIkkeMedlem = erAvklartSomIkkeMedlem(vurdertMedlemskap);

        return !(erAvklartMaskineltSomIkkeMedlem || erAvklartManueltSomIkkeMedlem);
    }

    private boolean erAvklartSomIkkeMedlem(Optional<VurdertLøpendeMedlemskapEntitet> medlemskap) {
        return medlemskap.isPresent() && medlemskap.get().getMedlemsperiodeManuellVurdering() != null
            && MedlemskapManuellVurderingType.UNNTAK.equals(medlemskap.get().getMedlemsperiodeManuellVurdering());
    }

    private boolean erAvklartSomPliktigEllerFrivillingMedlem(Optional<VurdertLøpendeMedlemskapEntitet> vurdertLøpendeMedlemskap,
                                                             Optional<MedlemskapAggregat> medlemskap, LocalDate vurderingsdato) {
        if (vurdertLøpendeMedlemskap.isPresent()) {
            VurdertMedlemskap vurdertMedlemskap = vurdertLøpendeMedlemskap.get();
            if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                MedlemskapManuellVurderingType.MEDLEM.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                return true;
            }
            if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                return false;
            }
        }
        return medTjeneste.brukerMaskineltAvklartSomFrivilligEllerPliktigMedlem(
            medlemskap.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()), vurderingsdato);
    }

    private boolean brukerNorskNordisk(PersonopplysningerAggregat personopplysningerAggregat, LocalDate vurderingsdato, LocalDate stp) {
        var søker = personopplysningerAggregat.getSøker();
        return personopplysningerAggregat.harStatsborgerskapRegionVedTidspunkt(søker.getAktørId(), Region.NORDEN, vurderingsdato, stp);
    }

    private RegelPersonStatusType tilPersonStatusType(PersonopplysningerAggregat personopplysningerAggregat, LocalDate vurderingsdato) {
        var søker = personopplysningerAggregat.getSøker();
        var personstatus = personopplysningerAggregat.getPersonstatusFor(søker.getAktørId(), SimpleLocalDateInterval.enDag(vurderingsdato)).getPersonstatus();
        return MAP_PERSONSTATUS_TYPE.get(personstatus);
    }

    private boolean finnOmSøkerHarArbeidsforholdOgInntekt(Behandling behandling, LocalDate vurderingsdato) {
        var inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandling.getId());

        if (inntektArbeidYtelseGrunnlagOptional.isPresent()) {
            var grunnlag = inntektArbeidYtelseGrunnlagOptional.get();
            var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandling.getAktørId())).før(vurderingsdato);

            if (filter.getYrkesaktiviteter().isEmpty()) {
                return false;
            }

            var arbeidsgivere = finnRelevanteArbeidsgivereMedLøpendeAvtaleEllerAvtaleSomErGyldigPåStp(vurderingsdato, filter);
            if (arbeidsgivere.isEmpty()) {
                return false;
            }

            var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(behandling.getAktørId())).før(vurderingsdato);

            return inntektFilter.filterPensjonsgivende().getAlleInntekter().stream()
                .anyMatch(e -> arbeidsgivere.contains(e.getArbeidsgiver()));
        }
        return false;
    }

    private List<Arbeidsgiver> finnRelevanteArbeidsgivereMedLøpendeAvtaleEllerAvtaleSomErGyldigPåStp(LocalDate skjæringstidspunkt, YrkesaktivitetFilter filter) {
        List<Arbeidsgiver> relevanteArbeid = new ArrayList<>();
        for (var yrkesaktivitet : filter.getYrkesaktiviteter()) {
            if (yrkesaktivitet.erArbeidsforhold()) {
                // Hvis har en løpende avtale fom før skjæringstidspunktet eller den som dekker skjæringstidspunktet
                var harLøpendeAvtaleFørSkjæringstidspunkt = filter.getAnsettelsesPerioder(yrkesaktivitet)
                    .stream()
                    .anyMatch(aktivitetsAvtale -> harLøpendeArbeidsforholdFørSkjæringstidspunkt(skjæringstidspunkt, aktivitetsAvtale));
                if (harLøpendeAvtaleFørSkjæringstidspunkt) {
                    relevanteArbeid.add(yrkesaktivitet.getArbeidsgiver());
                }
            }
        }
        return relevanteArbeid;
    }

    private boolean harLøpendeArbeidsforholdFørSkjæringstidspunkt(LocalDate skjæringstidspunkt, AktivitetsAvtale aktivitetsAvtale) {
        var fomDato = aktivitetsAvtale.getPeriode().getFomDato();
        var tomDato = aktivitetsAvtale.getPeriode().getTomDato();
        return aktivitetsAvtale.getErLøpende() && fomDato.isBefore(skjæringstidspunkt) || fomDato.isBefore(skjæringstidspunkt) && tomDato.isAfter(
            skjæringstidspunkt);
    }


    public boolean harOppholdstillatelsePåDato(BehandlingReferanse ref, LocalDate vurderingsdato) {
        if (ref.getUtledetMedlemsintervall().encloses(vurderingsdato)) {
            return personopplysningTjeneste.harOppholdstillatelseForPeriode(ref.behandlingId(), ref.getUtledetMedlemsintervall());
        }
        return personopplysningTjeneste.harOppholdstillatelsePåDato(ref.behandlingId(), vurderingsdato);
    }
}
