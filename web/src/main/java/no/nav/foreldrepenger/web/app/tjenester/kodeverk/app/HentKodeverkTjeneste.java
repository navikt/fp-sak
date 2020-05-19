package no.nav.foreldrepenger.web.app.tjenester.kodeverk.app;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBegrunnelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.kodeverk.BasisKodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeliste;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv.DokumentTypeIdKodeliste;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAndeltype;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@ApplicationScoped
public class HentKodeverkTjeneste {

    public static final Map<String, Collection<? extends Kodeverdi>> KODEVERDIER_SOM_BRUKES_PÅ_KLIENT;
    static {
        Map<String, Collection<? extends Kodeverdi>> map = new LinkedHashMap<>();

        map.put(RelatertYtelseTilstand.class.getSimpleName(), RelatertYtelseTilstand.kodeMap().values());
        map.put(FagsakStatus.class.getSimpleName(), FagsakStatus.kodeMap().values());
        map.put(RelatertYtelseType.class.getSimpleName(), RelatertYtelseType.kodeMap().values());
        map.put(BehandlingÅrsakType.class.getSimpleName(), BehandlingÅrsakType.kodeMap().values());
        map.put(KlageMedholdÅrsak.class.getSimpleName(), KlageMedholdÅrsak.kodeMap().values());
        map.put(KlageAvvistÅrsak.class.getSimpleName(), KlageAvvistÅrsak.kodeMap().values());
        map.put(HistorikkBegrunnelseType.class.getSimpleName(), HistorikkBegrunnelseType.kodeMap().values());
        map.put(OppgaveÅrsak.class.getSimpleName(), OppgaveÅrsak.kodeMap().values());
        map.put(MedlemskapManuellVurderingType.class.getSimpleName(), filtrerMedlemskapManuellVurderingType(MedlemskapManuellVurderingType.kodeMap().values()));
        map.put(BehandlingResultatType.class.getSimpleName(), BehandlingResultatType.kodeMap().values());
        map.put(VergeType.class.getSimpleName(), VergeType.kodeMap().values());
        map.put(VirksomhetType.class.getSimpleName(), VirksomhetType.kodeMap().values());
        map.put(PersonstatusType.class.getSimpleName(), PersonstatusType.kodeMap().values());
        map.put(FagsakYtelseType.class.getSimpleName(), FagsakYtelseType.kodeMap().values());
        map.put(FamilieHendelseType.class.getSimpleName(), FamilieHendelseType.kodeMap().values());
        map.put(Venteårsak.class.getSimpleName(), Venteårsak.kodeMap().values());
        map.put(ForeldreType.class.getSimpleName(), ForeldreType.kodeMap().values());
        map.put(InnsynResultatType.class.getSimpleName(), InnsynResultatType.kodeMap().values());
        map.put(BehandlingType.class.getSimpleName(), BehandlingType.kodeMap().values());
        map.put(ArbeidType.class.getSimpleName(), filtrerArbeidType(ArbeidType.kodeMap().values()));
        map.put(IkkeOppfyltÅrsak.class.getSimpleName(), IkkeOppfyltÅrsak.kodeMap().values());
        map.put(InnvilgetÅrsak.class.getSimpleName(), InnvilgetÅrsak.kodeMap().values());
        map.put(OpptjeningAktivitetType.class.getSimpleName(), OpptjeningAktivitetType.kodeMap().values());
        map.put(RevurderingVarslingÅrsak.class.getSimpleName(), RevurderingVarslingÅrsak.kodeMap().values());
        map.put(Inntektskategori.class.getSimpleName(), Inntektskategori.kodeMap().values());
        map.put(BeregningsgrunnlagAndeltype.class.getSimpleName(), BeregningsgrunnlagAndeltype.kodeMap().values());
        map.put(AktivitetStatus.class.getSimpleName(), AktivitetStatus.kodeMap().values());
        map.put(Arbeidskategori.class.getSimpleName(), Arbeidskategori.kodeMap().values());
        map.put(OmsorgsovertakelseVilkårType.class.getSimpleName(), OmsorgsovertakelseVilkårType.kodeMap().values());
        map.put(Fagsystem.class.getSimpleName(), Fagsystem.kodeMap().values());
        map.put(SivilstandType.class.getSimpleName(), SivilstandType.kodeMap().values());
        map.put(FaktaOmBeregningTilfelle.class.getSimpleName(), FaktaOmBeregningTilfelle.kodeMap().values());
        map.put(GraderingAvslagÅrsak.class.getSimpleName(), GraderingAvslagÅrsak.kodeMap().values());
        map.put(SkjermlenkeType.class.getSimpleName(), SkjermlenkeType.kodeMap().values());
        map.put(ArbeidsforholdHandlingType.class.getSimpleName(), ArbeidsforholdHandlingType.kodeMap().values());
        map.put(HistorikkOpplysningType.class.getSimpleName(), HistorikkOpplysningType.kodeMap().values());
        map.put(HistorikkEndretFeltType.class.getSimpleName(), HistorikkEndretFeltType.kodeMap().values());
        map.put(HistorikkinnslagType.class.getSimpleName(), HistorikkinnslagType.kodeMap().values());
        map.put(HistorikkAktør.class.getSimpleName(), HistorikkAktør.kodeMap().values());
        map.put(BehandlingStatus.class.getSimpleName(), BehandlingStatus.kodeMap().values());
        map.put(FarSøkerType.class.getSimpleName(), FarSøkerType.kodeMap().values());
        map.put(MedlemskapDekningType.class.getSimpleName(), MedlemskapDekningType.kodeMap().values());
        map.put(MedlemskapType.class.getSimpleName(), MedlemskapType.kodeMap().values());
        map.put(Avslagsårsak.class.getSimpleName(), Avslagsårsak.kodeMap().values());
        map.put(StønadskontoType.class.getSimpleName(), StønadskontoType.kodeMap().values());
        map.put(KonsekvensForYtelsen.class.getSimpleName(), KonsekvensForYtelsen.kodeMap().values());
        map.put(VilkårType.class.getSimpleName(), VilkårType.kodeMap().values());
        map.put(PermisjonsbeskrivelseType.class.getSimpleName(), PermisjonsbeskrivelseType.kodeMap().values());
        map.put(VurderArbeidsforholdHistorikkinnslag.class.getSimpleName(), VurderArbeidsforholdHistorikkinnslag.kodeMap().values());
        map.put(AnkeVurdering.class.getSimpleName(), AnkeVurdering.kodeMap().values());
        map.put(TilbakekrevingVidereBehandling.class.getSimpleName(), TilbakekrevingVidereBehandling.kodeMap().values());
        map.put(VurderÅrsak.class.getSimpleName(), VurderÅrsak.kodeMap().values());
        map.put(UttakUtsettelseType.class.getSimpleName(), UttakUtsettelseType.kodeMap().values());
        map.put(OppholdÅrsak.class.getSimpleName(), OppholdÅrsak.kodeMap().values());
        map.put(OverføringÅrsak.class.getSimpleName(), OverføringÅrsak.kodeMap().values());
        map.put(UtsettelseÅrsak.class.getSimpleName(), UtsettelseÅrsak.kodeMap().values());
        map.put(UttakArbeidType.class.getSimpleName(), UttakArbeidType.kodeMap().values());
        map.put(UttakPeriodeType.class.getSimpleName(), UttakPeriodeType.kodeMap().values());
        map.put(UttakPeriodeVurderingType.class.getSimpleName(), UttakPeriodeVurderingType.kodeMap().values());
        map.put(MorsAktivitet.class.getSimpleName(), MorsAktivitet.kodeMap().values());
        map.put(ManuellBehandlingÅrsak.class.getSimpleName(), ManuellBehandlingÅrsak.kodeMap().values());

        Map<String, Collection<? extends Kodeverdi>> mapFiltered = new LinkedHashMap<>();

        map.entrySet().forEach(e -> {
            mapFiltered.put(e.getKey(), e.getValue().stream().filter(f -> !"-".equals(f.getKode())).collect(Collectors.toSet()));
        });

        KODEVERDIER_SOM_BRUKES_PÅ_KLIENT = Collections.unmodifiableMap(mapFiltered);

    }
    public static final List<Class<? extends Kodeliste>> KODEVERK_SOM_BRUKES_PÅ_KLIENT = List.of(
        DokumentTypeIdKodeliste.class,
        Landkoder.class,
        Region.class);

    private KodeverkRepository kodeverkRepository;
    private BehandlendeEnhetTjeneste enhetsTjeneste;

    HentKodeverkTjeneste() {
        // for CDI proxy
    }

    private static Collection<? extends Kodeverdi> filtrerMedlemskapManuellVurderingType(Collection<MedlemskapManuellVurderingType> values) {
        return values.stream().filter(at -> at.visesPåKlient()).collect(Collectors.toSet());
    }

    private static Collection<? extends Kodeverdi> filtrerArbeidType(Collection<ArbeidType> values) {
        return values.stream().filter(at -> at.erAnnenOpptjening()).collect(Collectors.toSet());
    }

    @Inject
    public HentKodeverkTjeneste(KodeverkRepository kodeverkRepository, BehandlendeEnhetTjeneste enhetsTjeneste) {
        Objects.requireNonNull(kodeverkRepository, "kodeverkRepository"); //$NON-NLS-1$
        Objects.requireNonNull(enhetsTjeneste, "enhetsTjeneste"); //$NON-NLS-1$
        this.kodeverkRepository = kodeverkRepository;
        this.enhetsTjeneste = enhetsTjeneste;
    }

    public Map<String, Collection<? extends BasisKodeverdi>> hentGruppertKodeliste() {
        Map<String, Set<? extends Kodeliste>> kodelister = new HashMap<>(kodeverkRepository.hentAlle(KODEVERK_SOM_BRUKES_PÅ_KLIENT));

        // swap DokumentTypeId kodeliste og utvid med offisiellekoder der det er forskjell, så klient får navn for begge
        var dokumentTypeIdListe = kodelister.remove(DokumentTypeIdKodeliste.class.getSimpleName());
        kodelister.put(DokumentTypeId.class.getSimpleName(), utvidMedOffisielleKoder(dokumentTypeIdListe));

        // slå sammen kodeverdi og kodeliste maps
        Map<String, Collection<? extends BasisKodeverdi>> kodelistMap = new LinkedHashMap<>(kodelister);
        kodelistMap.putAll(KODEVERDIER_SOM_BRUKES_PÅ_KLIENT);

        return kodelistMap;

    }

    private Set<? extends Kodeliste> utvidMedOffisielleKoder(Collection<? extends Kodeliste> koder) {
            Set<Kodeliste> kodeverdier = new HashSet<>(koder);

            // double up med offisielle koder der offisiell er forskjellig fra kode slik at klienten får navn for begge og blir
            // mindre sårbar for switch fra uoffisiell til offisiell kode i backend
            var offisielleKodeverdier = kodeverdier.stream()
                .filter(k -> !Objects.equals(k.getKode(), k.getOffisiellKode()) && k.getOffisiellKode() != null)
                .map(k -> new MyOffisiellKodeKodeliste(k))
                .collect(Collectors.toSet());

            kodeverdier.addAll(offisielleKodeverdier);
            return kodeverdier;
    }

    public List<OrganisasjonsEnhet> hentBehandlendeEnheter() {
        return enhetsTjeneste.hentEnhetListe();
    }

    public static void main(String[] args) {
        for (var k : KODEVERK_SOM_BRUKES_PÅ_KLIENT) {
            if (!Kodeverdi.class.isAssignableFrom(k)) {
                System.out.println(k.getSimpleName() + ".class,");
            }
        }
    }

    @JsonFormat(shape = Shape.OBJECT)
    @JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
    public static class MyOffisiellKodeKodeliste extends Kodeliste {

        private String navn;

        public MyOffisiellKodeKodeliste(Kodeliste annen) {
            super(annen.getOffisiellKode(), annen.getKodeverk(), annen.getOffisiellKode(), annen.getGyldigFraOgMed(), annen.getGyldigTilOgMed());
            this.navn = annen.getNavn();
        }

        @Override
        public String getNavn() {
            return navn;
        }

    }
}
