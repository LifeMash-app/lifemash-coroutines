package com.lifemash.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapLatest

/**
 * 상위 Flow에서 새 값이 오면 이전 내부 Flow의 구독을 취소하고,
 * 새 내부 Flow를 구독하여 값을 전달합니다.
 *
 * 출력 버퍼를 두지 않으므로 소비자가 다음 값을 받을 준비가 될 때까지
 * 내부 Flow의 emit이 일시 중단됩니다.
 * 값을 전달한 뒤에는 소비자의 처리 완료를 기다리지 않고 진행할 수 있습니다.
 *
 * 이미 전달된 값의 처리나 저장된 상태를 취소하거나 되돌리지는 않습니다.
 * 후속 [buffer] 등의 연산자 결합에 따라 출력 버퍼 설정이 달라질 수 있습니다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun <T, R> Flow<T>.flatMapLatestUnbuffered(
    transform: suspend (T) -> Flow<R>,
): Flow<R> = flatMapLatest(transform).buffer(0)
