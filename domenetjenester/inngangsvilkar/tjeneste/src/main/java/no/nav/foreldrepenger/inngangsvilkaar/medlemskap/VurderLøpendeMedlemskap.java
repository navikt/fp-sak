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
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.Medlemskapsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.PersonStatusType;
import no.nav.fpsak.nare.evaluation.Evaluation;

@ApplicationScoped
public class VurderLøpendeMedlemskap {

    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private MedlemskapRepository medlemskapRepository;
    private InngangsvilkårOversetter inngangsvilkårOversetter;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private MedlemskapPerioderTjeneste medTjeneste;
    private UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerMedlemskap;

    VurderLøpendeMedlemskap() {
        //CDI
    }

    @Inject
    public VurderLøpendeMedlemskap(BasisPersonopplysningTjeneste personopplysningTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   MedlemskapRepository medlemskapRepository,
                                   InngangsvilkårOversetter inngangsvilkårOversetter,
                                   MedlemskapPerioderTjeneste medTjeneste,
                                   UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerMedlemskapTjeneste,
                                   InntektArbeidYtelseTjeneste iayTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.medlemskapRepository = medlemskapRepository;
        this.medTjeneste = medTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.utledVurderingsdatoerMedlemskap = utledVurderingsdatoerMedlemskapTjeneste;
    }

    public Map<LocalDate, VilkårData> vurderLøpendeMedlemskap(Long behandlingId) {
        Map<LocalDate, VilkårData> resultat = new TreeMap<>();

        for (Map.Entry<LocalDate, MedlemskapsvilkårGrunnlag> entry : lagGrunnlag(behandlingId).entrySet()) {
            VilkårData data = evaluerGrunnlag(entry.getValue());
            if (data.getUtfallType().equals(VilkårUtfallType.OPPFYLT)) {
                resultat.put(entry.getKey(), data);
            } else if (data.getUtfallType().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                if (data.getVilkårUtfallMerknad() == null) {
                    throw new IllegalStateException("Forventer at vilkår utfall merknad er satt når vilkåret blir satt til IKKE_OPPFYLT for grunnlag:" + entry.getValue().toString());
                }
                resultat.put(entry.getKey(), data);
                break;
            }
        }
        return resultat;
    }

    private VilkårData evaluerGrunnlag(MedlemskapsvilkårGrunnlag grunnlag) {
        Evaluation evaluation = new Medlemskapsvilkår().evaluer(grunnlag);
        return inngangsvilkårOversetter.tilVilkårData(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, evaluation, grunnlag);
    }

    private Map<LocalDate, MedlemskapsvilkårGrunnlag> lagGrunnlag(Long behandlingId) {
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskapPeriode = medlemskap.flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap);
        List<LocalDate> vurderingsdatoerListe = utledVurderingsdatoerMedlemskap.finnVurderingsdatoer(behandlingId)
            .stream()
            .sorted(LocalDate::compareTo)
            .collect(Collectors.toList());

        if (vurderingsdatoerListe.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<LocalDate, VurdertLøpendeMedlemskapEntitet> map = mapVurderingFraSaksbehandler(vurdertMedlemskapPeriode);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        Map<LocalDate, MedlemskapsvilkårGrunnlag> resulatat = new TreeMap<>();
        for (LocalDate vurderingsdato : vurderingsdatoerListe) {
            Optional<VurdertLøpendeMedlemskapEntitet> vurdertOpt = Optional.ofNullable(map.get(vurderingsdato));
            Optional<PersonopplysningerAggregat> aggregatOptional = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(ref.getBehandlingId(), ref.getAktørId(), vurderingsdato);
            MedlemskapsvilkårGrunnlag grunnlag = new MedlemskapsvilkårGrunnlag(
                brukerErMedlemEllerIkkeRelevantPeriode(medlemskap, vurdertOpt, aggregatOptional.get(), vurderingsdato), // FP VK 2.13
                tilPersonStatusType(aggregatOptional),
                brukerNorskNordisk(aggregatOptional),
                vurdertOpt.map(v -> defaultValueTrue(v.getErEøsBorger())).orElse(true)
            );

            grunnlag.setHarSøkerArbeidsforholdOgInntekt(finnOmSøkerHarArbeidsforholdOgInntekt(behandling, vurderingsdato));
            grunnlag.setBrukerAvklartLovligOppholdINorge(vurdertOpt.map(v -> defaultValueTrue(v.getLovligOppholdVurdering())).orElse(true));
            grunnlag.setBrukerAvklartBosatt(vurdertOpt.map(v -> defaultValueTrue(v.getBosattVurdering())).orElse(true));
            grunnlag.setBrukerAvklartOppholdsrett(vurdertOpt.map(v -> defaultValueTrue(v.getOppholdsrettVurdering())).orElse(true));
            grunnlag.setBrukerAvklartPliktigEllerFrivillig(erAvklartSomPliktigEllerFrivillingMedlem(vurdertOpt, medlemskap, vurderingsdato));

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
            Set<VurdertLøpendeMedlemskapEntitet> perioder = v.getPerioder();
            for (VurdertLøpendeMedlemskapEntitet vurdertLøpendeMedlemskap : perioder) {
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
        boolean erAvklartMaskineltSomIkkeMedlem = medTjeneste.brukerMaskineltAvklartSomIkkeMedlem(søker,
            medlemskapPerioder, vurderingsdato);
        boolean erAvklartManueltSomIkkeMedlem = erAvklartSomIkkeMedlem(vurdertMedlemskap);

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

    private boolean brukerNorskNordisk(Optional<PersonopplysningerAggregat> aggregatOptional) {
        if (aggregatOptional.isPresent()) {
            PersonopplysningerAggregat aggregat = aggregatOptional.get();
            final Optional<StatsborgerskapEntitet> statsborgerskap = aggregat.getStatsborgerskapFor(aggregat.getSøker().getAktørId()).stream().findFirst();
            return Region.NORDEN.equals(statsborgerskap.map(StatsborgerskapEntitet::getRegion).orElse(null));
        }
        return false;
    }

    private PersonStatusType tilPersonStatusType(Optional<PersonopplysningerAggregat> aggregatOptional) {
        if (aggregatOptional.isPresent()) {
            PersonopplysningerAggregat aggregat = aggregatOptional.get();
            PersonstatusType type = Optional.ofNullable(aggregat.getPersonstatusFor(aggregat.getSøker().getAktørId())).map(PersonstatusEntitet::getPersonstatus).orElse(null);

            if (PersonstatusType.BOSA.equals(type) || PersonstatusType.ADNR.equals(type)) {
                return PersonStatusType.BOSA;
            } else if (PersonstatusType.UTVA.equals(type)) {
                return PersonStatusType.UTVA;
            } else if (PersonstatusType.erDød(type)) {
                return PersonStatusType.DØD;
            }
        }
        return null;
    }

    private boolean finnOmSøkerHarArbeidsforholdOgInntekt(Behandling behandling, LocalDate vurderingsdato) {
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandling.getId());

        if (inntektArbeidYtelseGrunnlagOptional.isPresent()) {
            InntektArbeidYtelseGrunnlag grunnlag = inntektArbeidYtelseGrunnlagOptional.get();
            var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandling.getAktørId())).før(vurderingsdato);

            if (filter.getYrkesaktiviteter().isEmpty()) {
                return false;
            }

            List<Arbeidsgiver> arbeidsgivere = finnRelevanteArbeidsgivereMedLøpendeAvtaleEllerAvtaleSomErGyldigPåStp(vurderingsdato, filter);
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
        for (Yrkesaktivitet yrkesaktivitet : filter.getYrkesaktiviteter()) {
            if (yrkesaktivitet.erArbeidsforhold()) {
                // Hvis har en løpende avtale fom før skjæringstidspunktet eller den som dekker skjæringstidspunktet
                boolean harLøpendeAvtaleFørSkjæringstidspunkt = filter.getAnsettelsesPerioder(yrkesaktivitet)
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
        LocalDate fomDato = aktivitetsAvtale.getPeriode().getFomDato();
        LocalDate tomDato = aktivitetsAvtale.getPeriode().getTomDato();
        return (aktivitetsAvtale.getErLøpende() && fomDato.isBefore(skjæringstidspunkt))
            || (fomDato.isBefore(skjæringstidspunkt) && tomDato.isAfter(skjæringstidspunkt));
    }
}
