package no.nav.foreldrepenger.datavarehus.xml.svp;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.datavarehus.xml.YtelseXmlTjeneste;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.TilkjentYtelse;
import no.nav.vedtak.felles.xml.vedtak.ytelse.svp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.ytelse.svp.v2.YtelseSvangerskapspenger;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class YtelseXmlTjenesteImpl implements YtelseXmlTjeneste {
    BeregningsresultatRepository beregningsresultatRepository;
    private ObjectFactory ytelseObjectFactory;

    public YtelseXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public YtelseXmlTjenesteImpl(BehandlingRepositoryProvider repositoryProvider) {
        this.ytelseObjectFactory = new ObjectFactory();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    }

    @Override
    public void setYtelse(Beregningsresultat beregningsresultat, Behandling behandling) {
        //TODO PFP-7642 Implementere basert på YtelseXmlTjenesteForeldrepenger

        var ytelseSvangerskapspenger = ytelseObjectFactory.createYtelseSvangerskapspenger();

        var beregningsresultatOptional = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());
        if (beregningsresultatOptional.isPresent()) {
            setBeregningsresultat(ytelseSvangerskapspenger, beregningsresultatOptional.get().getBeregningsresultatPerioder());
        }
        var tilkjentYtelse = new TilkjentYtelse();
        tilkjentYtelse.getAny().add(ytelseObjectFactory.createYtelseSvangerskapspenger(ytelseSvangerskapspenger));
        beregningsresultat.setTilkjentYtelse(tilkjentYtelse);
    }

    private void setBeregningsresultat(YtelseSvangerskapspenger ytelseSvangerskapspenger, List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        var resultat = beregningsresultatPerioder
            .stream()
            .map(periode -> periode.getBeregningsresultatAndelList()).flatMap(andeler -> andeler.stream()).map(andel -> konverterFraDomene(andel)).collect(Collectors.toList());

        ytelseSvangerskapspenger.getBeregningsresultat().addAll(resultat);
    }

    private no.nav.vedtak.felles.xml.vedtak.ytelse.svp.v2.Beregningsresultat konverterFraDomene(BeregningsresultatAndel andelDomene) {
        var kontrakt = new no.nav.vedtak.felles.xml.vedtak.ytelse.svp.v2.Beregningsresultat();
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(andelDomene.getBeregningsresultatPeriode().getBeregningsresultatPeriodeFom(), andelDomene.getBeregningsresultatPeriode().getBeregningsresultatPeriodeTom()));
        kontrakt.setBrukerErMottaker(VedtakXmlUtil.lagBooleanOpplysning(andelDomene.erBrukerMottaker()));
        kontrakt.setAktivitetstatus(VedtakXmlUtil.lagKodeverksOpplysning(andelDomene.getAktivitetStatus()));
        kontrakt.setInntektskategori(VedtakXmlUtil.lagKodeverksOpplysning(andelDomene.getInntektskategori()));
        kontrakt.setDagsats(VedtakXmlUtil.lagIntOpplysning(andelDomene.getDagsats()));
        kontrakt.setStillingsprosent(VedtakXmlUtil.lagDecimalOpplysning(andelDomene.getStillingsprosent()));
        kontrakt.setUtbetalingsgrad(VedtakXmlUtil.lagDecimalOpplysning(andelDomene.getUtbetalingsgrad()));
        return kontrakt;
    }

}
