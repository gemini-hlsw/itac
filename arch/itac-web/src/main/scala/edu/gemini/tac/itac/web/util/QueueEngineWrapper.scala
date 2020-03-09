package edu.gemini.tac.itac.web.util

import org.springframework.stereotype.Service
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.service.{AbstractLogService => PsLogService, ICommitteeService => PsCommitteeService}
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import edu.gemini.tac.qservice.impl.QueueServiceImpl

trait QueueEngineWrapper {
  def fill(queue: PsQueue, committeService: PsCommitteeService, logService: PsLogService)
}

@Service(value = "queueEngineWrapper")
@Transactional
class QueueEngineWrapperImpl extends QueueEngineWrapper {

  def fill(queue: PsQueue, committeeService: PsCommitteeService, logService: PsLogService) {
    QueueServiceImpl.fill(queue, committeeService, logService)
  }
}
