package no.nav.foreldrepenger.datavarehus.xml.svp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.vedtak.felles.xml.vedtak.uttak.svp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.uttak.svp.v2.Tilrettelegging;
import no.nav.vedtak.felles.xml.vedtak.uttak.svp.v2.UttakSvangerskapspenger;
import no.nav.vedtak.felles.xml.vedtak.uttak.svp.v2.UttaksResultatArbeidsforhold;
import no.nav.vedtak.felles.xml.vedtak.uttak.svp.v2.UttaksresultatPeriode;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.Uttak;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class UttakXmlTjenesteImpl {

    SvangerskapspengerUttakResultatRepository uttakRepository;
    private ObjectFactory uttakObjectFactory;
    private SvangerskapspengerRepository svangerskapspengerRepository;

    public UttakXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public UttakXmlTjenesteImpl(BehandlingRepositoryProvider repositoryProvider) {
        this.uttakObjectFactory = new ObjectFactory();
        this.uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();
    }

    public void setUttak(Beregningsresultat beregningsresultat, Behandling behandling) {
        //TODO PFP-7642 Implementere basert på UttakXmlTjenesteForeldrepenger

        var uttakSvangerskapspenger = uttakObjectFactory.createUttakSvangerskapspenger();

        var svangerskapspengerUttakOptional = uttakRepository.hentHvisEksisterer(behandling.getId());

        svangerskapspengerUttakOptional.ifPresent(uttaksperiodegrense ->
            uttaksperiodegrense.finnFørsteUttaksdato().ifPresent(førsteUttaksdato ->
                VedtakXmlUtil.lagDateOpplysning(førsteUttaksdato).ifPresent(dateOpplysning -> uttakSvangerskapspenger.setFoersteUttaksdato(dateOpplysning))));

        svangerskapspengerUttakOptional.ifPresent(uttaksperiodegrense ->
            uttaksperiodegrense.finnSisteUttaksdato().ifPresent(sisteUttaksdato ->
                VedtakXmlUtil.lagDateOpplysning(sisteUttaksdato).ifPresent(dateOpplysning -> uttakSvangerskapspenger.setSisteUttaksdato(dateOpplysning))));

        svangerskapspengerUttakOptional.ifPresent(svangerskapspengerUttak ->
            setUttakUttaksResultatArbeidsforhold(uttakSvangerskapspenger, svangerskapspengerUttak.getUttaksResultatArbeidsforhold()));

        var svpGrunnlagEntitetOpt = svangerskapspengerRepository.hentGrunnlag(behandling.getId());
        svpGrunnlagEntitetOpt.ifPresent(svpGrunnlag -> setTilrettelegginger(uttakSvangerskapspenger,
            new TilretteleggingFilter(svpGrunnlagEntitetOpt.get()).getAktuelleTilretteleggingerFiltrert()));

        var uttak = new Uttak();
        uttak.getAny().add(uttakObjectFactory.createUttak(uttakSvangerskapspenger));
        beregningsresultat.setUttak(uttak);
    }


    private void setTilrettelegginger(UttakSvangerskapspenger uttakSvangerskapspenger, List<SvpTilretteleggingEntitet> svpTilrettelegginger) {
        var kontrakt = svpTilrettelegginger
            .stream()
            .map(svpTilrettelegging -> konverterFraDomene(svpTilrettelegging)).collect(Collectors.toList());
        uttakSvangerskapspenger.getTilrettelegging().addAll(kontrakt);
    }

    private Tilrettelegging konverterFraDomene(SvpTilretteleggingEntitet svpTilrettelegging) {
        //TODO PFP-8651: tilpass til å støtte mer enn en dato av hver type
        var kontrakt = new Tilrettelegging();

        var behovForTilretteleggingFomOptional = VedtakXmlUtil.lagDateOpplysning(svpTilrettelegging.getBehovForTilretteleggingFom());
        behovForTilretteleggingFomOptional.ifPresent(kontrakt::setBehovForTilretteleggingFom);

        var helTilretteleggingFomOptional = svpTilrettelegging.getTilretteleggingFOMListe().stream().filter(tl -> tl.getType().equals(TilretteleggingType.HEL_TILRETTELEGGING)).map(TilretteleggingFOM::getFomDato).max(LocalDate::compareTo);
        if (helTilretteleggingFomOptional.isPresent()) {
            var helTilretteleggingFom = VedtakXmlUtil.lagDateOpplysning(helTilretteleggingFomOptional.get());
            helTilretteleggingFom.ifPresent(kontrakt::setHelTilretteleggingFom);
        }

        var delvisTilretteleggingFomOptional = svpTilrettelegging.getTilretteleggingFOMListe().stream().filter(tl -> tl.getType().equals(TilretteleggingType.DELVIS_TILRETTELEGGING)).max(Comparator.comparing(TilretteleggingFOM::getFomDato));
        if (delvisTilretteleggingFomOptional.isPresent()) {
            var delvisTilretteleggingFom = VedtakXmlUtil.lagDateOpplysning(delvisTilretteleggingFomOptional.get().getFomDato());
            delvisTilretteleggingFom.ifPresent(kontrakt::setDelvisTilretteleggingFom);

            kontrakt.setStillingsprosent(VedtakXmlUtil.lagDecimalOpplysning(delvisTilretteleggingFomOptional.get().getStillingsprosent()));
        }

        var slutteArbeidFomOptional = svpTilrettelegging.getTilretteleggingFOMListe().stream().filter(tl -> tl.getType().equals(TilretteleggingType.INGEN_TILRETTELEGGING)).map(TilretteleggingFOM::getFomDato).max(LocalDate::compareTo);
        if (slutteArbeidFomOptional.isPresent()) {
            var slutteArbeidFom = VedtakXmlUtil.lagDateOpplysning(slutteArbeidFomOptional.get());
            slutteArbeidFom.ifPresent(kontrakt::setSlutteArbeidFom);
        }

        kontrakt.setArbeidtype(VedtakXmlUtil.lagKodeverksOpplysning(svpTilrettelegging.getArbeidType()));

        if (svpTilrettelegging.getOpplysningerOmRisikofaktorer().isPresent()) {
            kontrakt.setOpplysningerOmRisikofaktorer(VedtakXmlUtil.lagStringOpplysning(svpTilrettelegging.getOpplysningerOmRisikofaktorer().get()));
        }

        if (svpTilrettelegging.getOpplysningerOmTilretteleggingstiltak().isPresent()) {
            kontrakt.setOpplysningerOmTilretteleggingstiltak((VedtakXmlUtil.lagStringOpplysning(svpTilrettelegging.getOpplysningerOmTilretteleggingstiltak().get())));
        }

        if (svpTilrettelegging.getBegrunnelse().isPresent()) {
            kontrakt.setBegrunnelse((VedtakXmlUtil.lagStringOpplysning(svpTilrettelegging.getBegrunnelse().get())));
        }

        kontrakt.setKopiertFraTidligereBehandling(VedtakXmlUtil.lagBooleanOpplysning(svpTilrettelegging.getKopiertFraTidligereBehandling()));

        var mottattTidspunkt = VedtakXmlUtil.lagDateOpplysning(svpTilrettelegging.getMottattTidspunkt().toLocalDate());
        mottattTidspunkt.ifPresent(kontrakt::setMottattTidspunkt);

        var arbeidsgiverOptional = svpTilrettelegging.getArbeidsgiver();

        if (arbeidsgiverOptional.isPresent()) {
            kontrakt.setVirksomhet(VedtakXmlUtil.lagStringOpplysning(arbeidsgiverOptional.get().getOrgnr()));
            kontrakt.setArbeidsforholdid(VedtakXmlUtil.lagStringOpplysning(arbeidsgiverOptional.get().getIdentifikator()));
            kontrakt.setErVirksomhet(VedtakXmlUtil.lagBooleanOpplysning(arbeidsgiverOptional.get().getErVirksomhet()));
        } else{
            kontrakt.setErVirksomhet(VedtakXmlUtil.lagBooleanOpplysning(false));
        }

        return kontrakt;
    }


    private void setUttakUttaksResultatArbeidsforhold(UttakSvangerskapspenger uttakSvangerskapspenger, List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> arbeidsforholdDomene) {
        var kontrakt = arbeidsforholdDomene
            .stream()
            .map(periode -> konverterFraDomene(periode)).collect(Collectors.toList());
        uttakSvangerskapspenger.getUttaksResultatArbeidsforhold().addAll(kontrakt);
    }

    private UttaksResultatArbeidsforhold konverterFraDomene(SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold) {
        var kontrakt = new UttaksResultatArbeidsforhold();
        kontrakt.setVirksomhet(VedtakXmlUtil.lagStringOpplysning(arbeidsforhold.getArbeidsgiver()==null ? null:arbeidsforhold.getArbeidsgiver().getOrgnr()));
        kontrakt.setArbeidsforholdid(VedtakXmlUtil.lagStringOpplysning(arbeidsforhold.getId().toString()));
        setPerioder(kontrakt, arbeidsforhold.getPerioder());
        return kontrakt;
    }


    private void setPerioder(UttaksResultatArbeidsforhold kontraktArbeidsforhold, List<SvangerskapspengerUttakResultatPeriodeEntitet> perioder) {
        var kontrakt = perioder
            .stream()
            .map(periode -> konverterFraDomene(periode)).collect(Collectors.toList());
        kontraktArbeidsforhold.getUttaksresultatPerioder().addAll(kontrakt);
    }

    private UttaksresultatPeriode konverterFraDomene(SvangerskapspengerUttakResultatPeriodeEntitet periode) {
        var kontrakt = new UttaksresultatPeriode();
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periode.getFom(), periode.getTom()));

        var periodeResultatType = VedtakXmlUtil.lagKodeverksOpplysning(periode.getPeriodeResultatType());
        kontrakt.setPeriodeResultatType(periodeResultatType);

        var periodeIkkeOppfyltÅrsak = VedtakXmlUtil.lagKodeverksOpplysning(periode.getPeriodeIkkeOppfyltÅrsak());
        kontrakt.setPerioderesultataarsak(periodeIkkeOppfyltÅrsak);

        return kontrakt;
    }


}
