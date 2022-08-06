package no.nav.foreldrepenger.datavarehus.xml.es;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.datavarehus.xml.VilkårsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.RegelPersonStatusType;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Adopsjon;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.es.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.v2.Vilkaarsgrunnlag;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class VilkårsgrunnlagXmlTjenesteImpl extends VilkårsgrunnlagXmlTjeneste {

    private ObjectFactory vilkårObjectFactory = new ObjectFactory();
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public VilkårsgrunnlagXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public VilkårsgrunnlagXmlTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                                   KompletthetsjekkerProvider kompletthetsjekkerProvider,
                                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, kompletthetsjekkerProvider);
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;

    }

    @Override
    protected Vilkaarsgrunnlag getVilkaarsgrunnlag(Behandling behandling, Vilkår vilkårFraBehandling,
                                                   Optional<SøknadEntitet> søknad, Optional<LocalDate> familieHendelseDato) {
        Vilkaarsgrunnlag vilkaarsgrunnlag = null;
        if (VilkårType.SØKERSOPPLYSNINGSPLIKT.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForSøkersopplysningsplikt(behandling, søknad);
        } else if (VilkårType.MEDLEMSKAPSVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForMedlemskapsvilkåret(vilkårFraBehandling);
        } else if (VilkårType.FØDSELSVILKÅRET_MOR.equals(vilkårFraBehandling.getVilkårType()) || VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForFødselsvilkåret(vilkårFraBehandling);
        } else if (VilkårType.SØKNADSFRISTVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForSøknadsfristvilkåret(vilkårFraBehandling, søknad, familieHendelseDato);
        } else if ((VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD.equals(vilkårFraBehandling.getVilkårType())) || (VilkårType.ADOPSJONSVILKARET_FORELDREPENGER.equals(vilkårFraBehandling.getVilkårType()))) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForAdopsjonsvilkåret(vilkårFraBehandling);
        }
        return vilkaarsgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForAdopsjonsvilkåret(Vilkår vilkårFraBehandling) {
        var vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagAdopsjon();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }
        var grunnlagForVilkår = StandardJsonConfig.fromJson(
            vilkårFraBehandling.getRegelInput(), AdopsjonsvilkårGrunnlag.class);

        vilkårgrunnlag.setSoekersKjoenn(VedtakXmlUtil.lagStringOpplysning(grunnlagForVilkår.søkersKjønn().name()));
        var adopsjon = new Adopsjon();

        VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.omsorgsovertakelsesdato()).ifPresent(adopsjon::setOmsorgsovertakelsesdato);

        adopsjon.setErMannAdoptererAlene(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.mannAdoptererAlene()));

        adopsjon.setErEktefellesBarn(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.ektefellesBarn()));
        vilkårgrunnlag.setAdopsjon(adopsjon);
        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForSøknadsfristvilkåret(Vilkår vilkårFraBehandling,
                                                                        Optional<SøknadEntitet> søknadEntitet,
                                                                        Optional<LocalDate> familieHendelseDato) {
        var vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagSoeknadsfrist();
        søknadEntitet.map(SøknadEntitet::getElektroniskRegistrert).ifPresent(e -> vilkårgrunnlag.setElektroniskSoeknad(VedtakXmlUtil.lagBooleanOpplysning(e)));

        søknadEntitet.map(SøknadEntitet::getMottattDato).flatMap(VedtakXmlUtil::lagDateOpplysning).ifPresent(vilkårgrunnlag::setSoeknadMottattDato);

        familieHendelseDato.flatMap(VedtakXmlUtil::lagDateOpplysning).ifPresent(vilkårgrunnlag::setSkjaeringstidspunkt);

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForFødselsvilkåret(Vilkår vilkårFraBehandling) {
        var vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagFoedsel();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }
        var grunnlagForVilkår = StandardJsonConfig.fromJson(
            vilkårFraBehandling.getRegelInput(), FødselsvilkårGrunnlag.class);

        vilkårgrunnlag.setSokersKjoenn(VedtakXmlUtil.lagStringOpplysning(grunnlagForVilkår.søkersKjønn().name()));
        vilkårgrunnlag.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(grunnlagForVilkår.antallBarn()));

        Optional.ofNullable(grunnlagForVilkår.bekreftetFødselsdato()).flatMap(VedtakXmlUtil::lagDateOpplysning)
            .ifPresent(vilkårgrunnlag::setFoedselsdatoBarn);

        Optional.ofNullable(grunnlagForVilkår.terminbekreftelseTermindato()).flatMap(VedtakXmlUtil::lagDateOpplysning)
            .ifPresent(vilkårgrunnlag::setTermindato);

        Optional.ofNullable(grunnlagForVilkår.søkerRolle()).map(RegelSøkerRolle::name)
            .map(VedtakXmlUtil::lagStringOpplysning).ifPresent(vilkårgrunnlag::setSoekersRolle);

        Optional.ofNullable(grunnlagForVilkår.behandlingsdato()).flatMap(VedtakXmlUtil::lagDateOpplysning)
            .ifPresent(vilkårgrunnlag::setSoeknadsdato);

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForMedlemskapsvilkåret(Vilkår vilkårFraBehandling) {
        var vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagMedlemskap();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }
        var grunnlagForVilkår = StandardJsonConfig.fromJson(
            vilkårFraBehandling.getRegelInput(),
            MedlemskapsvilkårGrunnlag.class
        );
        vilkårgrunnlag.setErBrukerBorgerAvEUEOS(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerBorgerAvEUEOS()));
        vilkårgrunnlag.setHarBrukerLovligOppholdINorge(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerAvklartLovligOppholdINorge()));
        vilkårgrunnlag.setHarBrukerOppholdsrett(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerAvklartOppholdsrett()));
        vilkårgrunnlag.setErBrukerBosatt(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerAvklartBosatt()));
        vilkårgrunnlag.setErBrukerNordiskstatsborger(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerNorskNordisk()));
        vilkårgrunnlag.setErBrukerPliktigEllerFrivilligMedlem(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerAvklartPliktigEllerFrivillig()));
        vilkårgrunnlag.setErBrukerMedlem(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerErMedlem()));
        vilkårgrunnlag.setPersonstatus(VedtakXmlUtil.lagStringOpplysning(
            Optional.ofNullable(grunnlagForVilkår.personStatusType()).map(RegelPersonStatusType::getNavn).orElse("-")));

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForSøkersopplysningsplikt(Behandling behandling, Optional<SøknadEntitet> optionalSøknad) {
        boolean komplettSøknad;
        boolean elektroniskSøknad;
        boolean erBarnetFødt;
        if (!optionalSøknad.isPresent()) {
            komplettSøknad = false;
            elektroniskSøknad = false;
            erBarnetFødt = false;
        } else {
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
            var søknad = optionalSøknad.get();
            komplettSøknad = erKomplettSøknad(ref);
            elektroniskSøknad = søknad.getElektroniskRegistrert();
            erBarnetFødt = erBarnetFødt(behandling);
        }
        var vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagSoekersopplysningsplikt();
        vilkårgrunnlag.setErSoeknadenKomplett(VedtakXmlUtil.lagBooleanOpplysning(komplettSøknad)); //Denne er unødvendig fo dvh.
        vilkårgrunnlag.setElektroniskSoeknad(VedtakXmlUtil.lagBooleanOpplysning(elektroniskSøknad));
        vilkårgrunnlag.setErBarnetFoedt(VedtakXmlUtil.lagBooleanOpplysning(erBarnetFødt));
        return vilkårgrunnlag;
    }
}
