package no.nav.foreldrepenger.inngangsvilkaar.adopsjon;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.BekreftetAdopsjon;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.BekreftetAdopsjonBarn;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class AdopsjonsvilkårOversetter {

    private static final Map<NavBrukerKjønn, RegelKjønn> MAP_KJØNN = Map.of(
        NavBrukerKjønn.KVINNE, RegelKjønn.KVINNE,
        NavBrukerKjønn.MANN, RegelKjønn.MANN
    );

    private FamilieHendelseRepository familieGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;

    AdopsjonsvilkårOversetter() {
        // for CDI proxy
    }

    @Inject
    public AdopsjonsvilkårOversetter(BehandlingRepositoryProvider repositoryProvider,
                                     PersonopplysningTjeneste personopplysningTjeneste) {
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public AdopsjonsvilkårGrunnlag oversettTilRegelModellAdopsjon(BehandlingReferanse ref) {
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(ref.behandlingId());
        var bekreftetAdopsjon = byggBekreftetAdopsjon(ref, familieHendelseGrunnlag);
        var adopsjonBarn = bekreftetAdopsjon.adopsjonBarn();
        return new AdopsjonsvilkårGrunnlag(
            adopsjonBarn,
            bekreftetAdopsjon.ektefellesBarn(),
            tilSøkerKjøenn(getSøkersKjønn(ref)),
            bekreftetAdopsjon.adoptererAlene(),
            bekreftetAdopsjon.omsorgsovertakelseDato(),
            false);
    }

    private static BekreftetAdopsjon byggBekreftetAdopsjon(BehandlingReferanse ref, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var bekreftetVersjon = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon();
        var adopsjon = bekreftetVersjon.flatMap(FamilieHendelseEntitet::getAdopsjon)
            .orElseThrow(() -> new TekniskException("FP-384255",
                String.format("Ikke mulig å oversette adopsjonsgrunnlag til regelmotor for behandlingId %s", ref.behandlingId())));

        var bekreftetAdopsjonBarn = bekreftetVersjon.map(FamilieHendelseEntitet::getBarna)
            .orElse(List.of())
            .stream()
            .map(barn -> new BekreftetAdopsjonBarn(barn.getFødselsdato()))
            .toList();
        return new BekreftetAdopsjon(adopsjon.getOmsorgsovertakelseDato(), bekreftetAdopsjonBarn,
            getBooleanOrDefaultFalse(adopsjon.getErEktefellesBarn()), getBooleanOrDefaultFalse(adopsjon.getAdoptererAlene()));
    }

    private static boolean getBooleanOrDefaultFalse(Boolean bool) {
        if (bool == null) {
            return false;
        }
        return bool;
    }

    private NavBrukerKjønn getSøkersKjønn(BehandlingReferanse ref) {
        try {
            return personopplysningTjeneste.hentPersonopplysninger(ref).getSøker().getKjønn();
        } catch (Exception e) {
            return NavBrukerKjønn.UDEFINERT;
        }
    }


    private static RegelKjønn tilSøkerKjøenn(NavBrukerKjønn søkerKjønn) {
        return Optional.ofNullable(MAP_KJØNN.get(søkerKjønn))
            .orElseThrow(() -> new NullPointerException("Fant ingen kjønn for " + søkerKjønn.getKode()));
    }

}
