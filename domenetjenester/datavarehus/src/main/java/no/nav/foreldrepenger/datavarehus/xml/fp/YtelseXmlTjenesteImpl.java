package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.datavarehus.xml.YtelseXmlTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.TilkjentYtelse;
import no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.Virksomhet;
import no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.YtelseForeldrepenger;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class YtelseXmlTjenesteImpl implements YtelseXmlTjeneste {
    private ObjectFactory ytelseObjectFactory;

    private BeregningsresultatRepository beregningsresultatRepository;

    private VirksomhetTjeneste virksomhetTjeneste;

    protected YtelseXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public YtelseXmlTjenesteImpl(BehandlingRepositoryProvider repositoryProvider, VirksomhetTjeneste virksomhetTjeneste) {
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.ytelseObjectFactory = new ObjectFactory();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    }

    @Override
    public void setYtelse(Beregningsresultat beregningsresultat, Behandling behandling) {
        var ytelseForeldrepenger = ytelseObjectFactory.createYtelseForeldrepenger();
        var beregningsresultatOptional = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());
        if (beregningsresultatOptional.isPresent()) {
            setBeregningsresultat(ytelseForeldrepenger, beregningsresultatOptional.get().getBeregningsresultatPerioder());
        }
        var tilkjentYtelse = new TilkjentYtelse();
        tilkjentYtelse.getAny().add(ytelseObjectFactory.createYtelseForeldrepenger(ytelseForeldrepenger));
        beregningsresultat.setTilkjentYtelse(tilkjentYtelse);
    }

    private void setBeregningsresultat(YtelseForeldrepenger ytelseForeldrepenger, List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        var resultat = beregningsresultatPerioder
            .stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatAndelList).flatMap(Collection::stream).map(this::konverterFraDomene).toList();

        ytelseForeldrepenger.getBeregningsresultat().addAll(resultat);
    }

    private no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.Beregningsresultat konverterFraDomene(BeregningsresultatAndel andelDomene) {
        var kontrakt = new no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.Beregningsresultat();
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(andelDomene.getBeregningsresultatPeriode().getBeregningsresultatPeriodeFom(), andelDomene.getBeregningsresultatPeriode().getBeregningsresultatPeriodeTom()));
        kontrakt.setBrukerErMottaker(VedtakXmlUtil.lagBooleanOpplysning(andelDomene.erBrukerMottaker()));
        kontrakt.setVirksomhet(konverterVirksomhetFraDomene(andelDomene));
        kontrakt.setAktivitetstatus(VedtakXmlUtil.lagKodeverksOpplysning(andelDomene.getAktivitetStatus()));
        kontrakt.setInntektskategori(VedtakXmlUtil.lagKodeverksOpplysning(andelDomene.getInntektskategori()));
        kontrakt.setDagsats(VedtakXmlUtil.lagIntOpplysning(andelDomene.getDagsats()));
        kontrakt.setStillingsprosent(VedtakXmlUtil.lagDecimalOpplysning(andelDomene.getStillingsprosent()));
        kontrakt.setUtbetalingsgrad(VedtakXmlUtil.lagDecimalOpplysning(andelDomene.getUtbetalingsgrad()));
        return kontrakt;
    }

    private Virksomhet konverterVirksomhetFraDomene(BeregningsresultatAndel andelDomene) {
        var kontrakt = new Virksomhet();
        andelDomene.getArbeidsgiver().map(Arbeidsgiver::getOrgnr).ifPresent(orgNr -> {
            kontrakt.setOrgnr(VedtakXmlUtil.lagStringOpplysning(orgNr));
            if (!OrgNummer.erKunstig(orgNr)) {
                var virksomhet = virksomhetTjeneste.finnOrganisasjon(orgNr);
                kontrakt.setNavn(VedtakXmlUtil.lagStringOpplysning(virksomhet.orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + orgNr)).getNavn()));
            } else {
                kontrakt.setNavn(VedtakXmlUtil.lagStringOpplysning("Kunstig virksomhet"));
            }
        });

        Optional.ofNullable(andelDomene.getArbeidsforholdRef()).ifPresent(ref -> kontrakt.setArbeidsforholdid(VedtakXmlUtil.lagStringOpplysning(ref.getReferanse())));
        return kontrakt;
    }
}
