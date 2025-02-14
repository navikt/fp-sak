package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.datavarehus.xml.VilkårsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v1.MedlemskapsvilkårGrunnlagV1;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårParametre;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.v2.Vilkaarsgrunnlag;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class VilkårsgrunnlagXmlTjenesteImpl extends VilkårsgrunnlagXmlTjeneste {

    private ObjectFactory vilkårObjectFactory = new ObjectFactory();
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public VilkårsgrunnlagXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public VilkårsgrunnlagXmlTjenesteImpl(BehandlingRepositoryProvider repositoryProvider, Kompletthetsjekker kompletthetsjekker, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, kompletthetsjekker);
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    protected Vilkaarsgrunnlag getVilkaarsgrunnlag(Behandling behandling, Vilkår vilkårFraBehandling,
                                                   Optional<SøknadEntitet> søknad,
                                                   @SuppressWarnings("unused") Optional<LocalDate> familieHendelseDato) {
        Vilkaarsgrunnlag vilkaarsgrunnlag = null;
        if (VilkårType.FØDSELSVILKÅRET_MOR.equals(vilkårFraBehandling.getVilkårType()) || VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForFødselsvilkåret(behandling, vilkårFraBehandling);
        } else if (VilkårType.ADOPSJONSVILKARET_FORELDREPENGER.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForAdopsjonsvilkåret(vilkårFraBehandling);
        } else if (VilkårType.SØKERSOPPLYSNINGSPLIKT.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForSøkersopplysningsplikt(behandling, søknad);
        } else if (VilkårType.MEDLEMSKAPSVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForMedlemskapsvilkåret(vilkårFraBehandling);
        } else if (VilkårType.OPPTJENINGSVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForOpptjening(vilkårFraBehandling);
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

        if (RegelKjønn.KVINNE.equals(grunnlagForVilkår.søkersKjønn())) {
            vilkårgrunnlag.setSoekersKjoenn(VedtakXmlUtil.lagKodeverksOpplysning(NavBrukerKjønn.KVINNE));
        } else {
            vilkårgrunnlag.setSoekersKjoenn(VedtakXmlUtil.lagKodeverksOpplysning(NavBrukerKjønn.MANN));
        }
        VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.omsorgsovertakelsesdato()).ifPresent(vilkårgrunnlag::setOmsorgsovertakelsesdato);

        vilkårgrunnlag.setMannAdoptererAlene(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.mannAdoptererAlene()));
        vilkårgrunnlag.setEktefellesBarn(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.ektefellesBarn()));

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForFødselsvilkåret(Behandling behandling, Vilkår vilkårFraBehandling) {
        var vilkårgrunnlagFødselForeldrepenger = vilkårObjectFactory.createVilkaarsgrunnlagFoedsel();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlagFødselForeldrepenger;
        }
        var grunnlagForVilkår = StandardJsonConfig.fromJson(
            vilkårFraBehandling.getRegelInput(), FødselsvilkårGrunnlag.class);

        vilkårgrunnlagFødselForeldrepenger.setSokersKjoenn(VedtakXmlUtil.lagStringOpplysning(grunnlagForVilkår.søkersKjønn().name()));
        vilkårgrunnlagFødselForeldrepenger.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(grunnlagForVilkår.antallBarn()));

        Optional.ofNullable(grunnlagForVilkår.bekreftetFødselsdato()).flatMap(VedtakXmlUtil::lagDateOpplysning)
            .ifPresent(vilkårgrunnlagFødselForeldrepenger::setFoedselsdatoBarn);

        Optional.ofNullable(grunnlagForVilkår.terminbekreftelseTermindato()).flatMap(VedtakXmlUtil::lagDateOpplysning)
            .ifPresent(vilkårgrunnlagFødselForeldrepenger::setTermindato);

        Optional.ofNullable(grunnlagForVilkår.søkerRolle()).map(RegelSøkerRolle::name)
            .map(VedtakXmlUtil::lagStringOpplysning).ifPresent(vilkårgrunnlagFødselForeldrepenger::setSoekersRolle);

        Optional.ofNullable(grunnlagForVilkår.behandlingsdato()).flatMap(VedtakXmlUtil::lagDateOpplysning)
            .ifPresent(vilkårgrunnlagFødselForeldrepenger::setSoeknadsdato);

        familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::erMorForSykVedFødsel)
            .map(VedtakXmlUtil::lagBooleanOpplysning)
            .ifPresent(vilkårgrunnlagFødselForeldrepenger::setErMorForSykVedFodsel);

        return vilkårgrunnlagFødselForeldrepenger;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForMedlemskapsvilkåret(Vilkår vilkårFraBehandling) {
        var vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagMedlemskap();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }
        var grunnlagForVilkår = StandardJsonConfig.fromJson(
            vilkårFraBehandling.getRegelInput(),
            MedlemskapsvilkårGrunnlagV1.class
        );
        vilkårgrunnlag.setErBrukerBorgerAvEUEOS(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerBorgerAvEUEOS()));
        vilkårgrunnlag.setHarBrukerLovligOppholdINorge(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerAvklartLovligOppholdINorge()));
        vilkårgrunnlag.setHarBrukerOppholdsrett(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerAvklartOppholdsrett()));
        vilkårgrunnlag.setErBrukerBosatt(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerAvklartBosatt()));
        vilkårgrunnlag.setErBrukerNordiskstatsborger(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerNorskNordisk()));
        vilkårgrunnlag.setErBrukerPliktigEllerFrivilligMedlem(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerAvklartPliktigEllerFrivillig()));
        vilkårgrunnlag.setErBrukerMedlem(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.brukerErMedlem()));
        vilkårgrunnlag.setPersonstatus(VedtakXmlUtil.lagStringOpplysning(
            Optional.ofNullable(grunnlagForVilkår.personStatusType()).map(MedlemskapsvilkårGrunnlagV1.RegelPersonStatusType::getNavn).orElse("-")
        ));
        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForOpptjening(Vilkår vilkårFraBehandling) {
        var vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagOpptjening();

        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }

        var opptjeningsgrunnlag = StandardJsonConfig.fromJson(
            vilkårFraBehandling.getRegelInput(),
            Opptjeningsgrunnlag.class
        );
        var opptjeningsparametre = OpptjeningsvilkårParametre.opptjeningsparametreForeldrepenger();

        if (opptjeningsgrunnlag != null) {
            VedtakXmlUtil.lagDateOpplysning(opptjeningsgrunnlag.behandlingsDato()).ifPresent(vilkårgrunnlag::setBehandlingsDato);

            vilkårgrunnlag.setMinsteAntallDagerGodkjent(VedtakXmlUtil.lagIntOpplysning(opptjeningsparametre.minsteAntallDagerGodkjent()));
            vilkårgrunnlag.setMinsteAntallMånederGodkjent(VedtakXmlUtil.lagIntOpplysning(opptjeningsparametre.minsteAntallMånederGodkjent()));
            var opptjeningsperiode = opptjeningsgrunnlag.getOpptjeningPeriode();
            vilkårgrunnlag.setOpptjeningperiode(VedtakXmlUtil.lagPeriodeOpplysning(opptjeningsperiode.getFomDato(), opptjeningsperiode.getTomDato()));
            vilkårgrunnlag.setMinsteInntekt(VedtakXmlUtil.lagLongOpplysning(opptjeningsparametre.minsteInntekt()));

            vilkårgrunnlag.setMaksMellomliggendePeriodeForArbeidsforhold(VedtakXmlUtil.lagStringOpplysningForperiode(opptjeningsparametre.maksMellomliggendePeriodeForArbeidsforhold()));
            vilkårgrunnlag.setMinForegaaendeForMellomliggendePeriodeForArbeidsforhold(VedtakXmlUtil.lagStringOpplysningForperiode(opptjeningsparametre.minForegåendeForMellomliggendePeriodeForArbeidsforhold()));
            vilkårgrunnlag.setPeriodeAntattGodkjentForBehandlingstidspunkt(VedtakXmlUtil.lagStringOpplysningForperiode(opptjeningsparametre.periodeAntattGodkjentFørBehandlingstidspunkt()));
        }

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForSøkersopplysningsplikt(Behandling behandling, Optional<SøknadEntitet> optionalSøknad) {
        boolean elektroniskSøknad;
        LocalDate mottattDato;
        LocalDate skjæringstidspunkt;
        if (optionalSøknad.isEmpty()) {
            elektroniskSøknad = false;
            mottattDato = null;
            skjæringstidspunkt = null;
        } else {
            var søknad = optionalSøknad.get();
            mottattDato = getMottattDato(behandling);
            elektroniskSøknad = søknad.getElektroniskRegistrert();
            skjæringstidspunkt = getSkjæringstidsunkt(behandling.getId());
        }
        var vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagSoekersopplysningsplikt();

        vilkårgrunnlag.setElektroniskSoeknad(VedtakXmlUtil.lagBooleanOpplysning(elektroniskSøknad));
        VedtakXmlUtil.lagDateOpplysning(mottattDato).ifPresent(vilkårgrunnlag::setSoeknadMottatDato);
        VedtakXmlUtil.lagDateOpplysning(skjæringstidspunkt).ifPresent(vilkårgrunnlag::setSkjaeringstidspunkt);
        return vilkårgrunnlag;
    }

    private LocalDate getSkjæringstidsunkt(Long behandlingId) {
        return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
    }
}
